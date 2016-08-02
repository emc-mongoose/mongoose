package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.common.concurrent.BlockingQueueTaskSequencer;
import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Balancer;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.impl.load.BasicBalancer;
import com.emc.mongoose.storage.driver.base.DriverBase;

import static com.emc.mongoose.common.concurrent.BlockingQueueTaskSequencer.INSTANCE;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 29.07.16.
 */
public abstract class HttpDriverBase<I extends Item, O extends IoTask<I>>
extends DriverBase<I, O>
implements Driver<I, O> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	private final String storageNodeAddrs[];
	private final int storageNodePort;
	private final Balancer<String> storageNodeBalancer;
	
	private final EventLoopGroup workerGroup;
	protected final Bootstrap bootstrap;
	
	protected HttpDriverBase(
		final LoadConfig loadConfig, final StorageConfig storageConfig,
		final SocketConfig socketConfig
	) throws UserShootHisFootException {
		
		super(loadConfig);
		storageNodeAddrs = (String[]) storageConfig.getAddresses().toArray();
		storageNodePort = storageConfig.getPort();
		storageNodeBalancer = new BasicBalancer<>(storageNodeAddrs);
		
		final SimpleChannelInboundHandler<HttpObject> apiSpecificHandler = getApiSpecificHandler();
		
		workerGroup = new EpollEventLoopGroup(0, new NamingThreadFactory("test"));
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(EpollSocketChannel.class);
		//bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator)
		//bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE)
		//bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, )
		//bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR)
		//bootstrap.option(ChannelOption.AUTO_READ)
		//bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS)
		//bootstrap.option(ChannelOption.SO_RCVBUF);
		//bootstrap.option(ChannelOption.SO_SNDBUF);
		bootstrap.option(ChannelOption.SO_BACKLOG, socketConfig.getBindBackLogSize());
		bootstrap.option(ChannelOption.SO_KEEPALIVE, socketConfig.getKeepAlive());
		bootstrap.option(ChannelOption.SO_LINGER, socketConfig.getLinger());
		bootstrap.option(ChannelOption.SO_REUSEADDR, socketConfig.getReuseAddr());
		bootstrap.option(ChannelOption.SO_TIMEOUT, socketConfig.getTimeoutMillisec());
		bootstrap.option(ChannelOption.TCP_NODELAY, socketConfig.getTcpNoDelay());
		bootstrap.handler(
			new HttpClientChannelInitializer(storageConfig.getSsl(), apiSpecificHandler)
		);
	}
	
	protected abstract SimpleChannelInboundHandler<HttpObject> getApiSpecificHandler();
	
	protected abstract HttpRequest getDataRequest(final O ioTask);
	
	protected abstract HttpRequest getRequest(final O ioTask);
	
	private final class HttpRequestFuture
	extends FutureTaskBase {
		
		private final HttpRequest httpRequest;
		
		public HttpRequestFuture(final HttpRequest httpRequest) {
			this.httpRequest = httpRequest;
		}
		
		@Override
		public final void run() {
			
			final String bestNode;
			if(storageNodeAddrs.length == 1) {
				bestNode = storageNodeAddrs[0];
			} else {
				try {
					bestNode = storageNodeBalancer.get();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to get the best node");
					return;
				}
			}
			
			final Channel c;
			try {
				c = bootstrap.connect(bestNode, storageNodePort).sync().channel();
			} catch(final InterruptedException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to get the connection to \"{}\"", bestNode
				);
				return;
			}
			
			c.writeAndFlush(httpRequest);
		}
	}
	
	
	@Override
	public void submit(final O task)
	throws InterruptedException {
		
		final HttpRequest httpRequest;
		if(task instanceof DataIoTask) {
			httpRequest = getDataRequest(task);
		} else {
			httpRequest = getRequest(task);
		}
		
		INSTANCE.submit(new HttpRequestFuture(httpRequest));
	}
	
	@Override
	public int submit(final List<O> tasks, final int from, final int to)
	throws InterruptedException {
		return 0;
	}
	
	@Override
	public boolean await()
	throws InterruptedException {
		return false;
	}
	
	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}
	
	@Override
	public boolean isFullThrottleEntered() {
		return false;
	}
	
	@Override
	public boolean isFullThrottleExited() {
		return false;
	}
	
	@Override
	protected void doStart()
	throws UserShootHisFootException {
	}
	
	@Override
	protected void doShutdown()
	throws UserShootHisFootException {
	}
	
	@Override
	protected void doInterrupt()
	throws UserShootHisFootException {
		final Future f = workerGroup.shutdownGracefully(0, 1, TimeUnit.NANOSECONDS);
		try {
			f.await(1, TimeUnit.SECONDS);
		} catch(final InterruptedException e) {
			LOG.warn(Markers.ERR, "Failed to interrupt the HTTP storage driver gracefully");
		}
	}
	
	@Override
	public void close()
	throws IOException {
		super.close();
	}
}
