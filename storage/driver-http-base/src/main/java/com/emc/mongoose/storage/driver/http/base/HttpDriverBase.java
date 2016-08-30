package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.Balancer;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.impl.io.AsyncCurrentDateInput;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.impl.item.BasicMutableDataItem;
import com.emc.mongoose.model.impl.load.BasicBalancer;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.storage.driver.base.DriverBase;

import static com.emc.mongoose.common.concurrent.BlockingQueueTaskSequencer.INSTANCE;
import static com.emc.mongoose.model.api.item.Item.SLASH;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

import com.emc.mongoose.storage.driver.http.base.data.DataItemFileRegion;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
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
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(runId, loadConfig);
		storageNodeAddrs = storageConfig.getAddrs().toArray(new String[]{});
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
		//bootstrap.option(ChannelOption.SO_BACKLOG, socketConfig.getBindBackLogSize());
		bootstrap.option(ChannelOption.SO_KEEPALIVE, socketConfig.getKeepAlive());
		bootstrap.option(ChannelOption.SO_LINGER, socketConfig.getLinger());
		bootstrap.option(ChannelOption.SO_REUSEADDR, socketConfig.getReuseAddr());
		//bootstrap.option(ChannelOption.SO_TIMEOUT, socketConfig.getTimeoutMillisec());
		bootstrap.option(ChannelOption.TCP_NODELAY, socketConfig.getTcpNoDelay());
		bootstrap.handler(
			new HttpClientChannelInitializer(storageConfig.getSsl(), apiSpecificHandler)
		);
	}
	
	protected abstract SimpleChannelInboundHandler<HttpObject> getApiSpecificHandler();
	
	protected HttpRequest getHttpRequest(final O ioTask, final String nodeAddr) {
		final I item = ioTask.getItem();
		final LoadType ioType = ioTask.getLoadType();
		final HttpMethod httpMethod = getHttpMethod(ioType);
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, getUriPath(item, ioTask), httpHeaders
		);
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		switch(ioType) {
			case CREATE:
				//if(srcContainer == null) {
				//	httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, item.)
				//} else {
				//	applyCopyHeaders(httpHeaders);
				//}
				break;
			case READ:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
			case UPDATE:
				// TODO if data item set ranges headers conditionally
				break;
			case DELETE:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
		}
		applyMetaDataHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders);
		return httpRequest;
	}
	
	protected HttpMethod getHttpMethod(final LoadType loadType) {
		switch(loadType) {
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}
	
	protected String getUriPath(final I item, final O ioTask) {
		return SLASH + ioTask.getDstPath() + SLASH + item.getName();
	}
	
	protected abstract void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException;
	
	protected abstract String getObjectSrcPath(final I object)
	throws URISyntaxException;
	
	protected void applySharedHeaders(final HttpHeaders httpHeaders) {
		// TODO apply dynamic headers also
	}
	
	protected void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}
	
	protected void applyAuthHeaders(final HttpHeaders httpHeaders) {
	}
	
	private final class HttpRequestFuture
	extends FutureTaskBase {
		
		private final O ioTask;
		
		public HttpRequestFuture(final O ioTask) {
			this.ioTask = ioTask;
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
			
			c.write(getHttpRequest(ioTask, bestNode));
			
			final LoadType ioType = ioTask.getLoadType();
			final I item = ioTask.getItem();
			
			try {
				if(LoadType.CREATE.equals(ioType)) {
					if(item instanceof DataItem) {
						c.write(new DataItemFileRegion<>((DataItem) item));
					}
				} else if(LoadType.UPDATE.equals(ioType)) {
					if(item instanceof MutableDataItem) {
						final MutableDataItem mdi = (MutableDataItem) item;
						if(mdi.hasScheduledUpdates()) {
							long nextRangeOffset, nextRangeSize;
							BasicDataItem nextUpdatedRange;
							for(int i = 0; i < mdi.getCountRangesTotal(); i ++) {
								if(mdi.isCurrLayerRangeUpdating(i)) {
									nextRangeOffset = BasicMutableDataItem.getRangeOffset(i);
									nextRangeSize = mdi.getRangeSize(i);
									nextUpdatedRange = new BasicDataItem(
										(BasicDataItem) mdi, nextRangeOffset, nextRangeSize, true
									);
									c.write(new DataItemFileRegion<>(nextUpdatedRange));
								}
							}
						}
					}
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to write the data");
			}
			
			c.writeAndFlush(EMPTY_LAST_CONTENT);
		}
	}
	
	
	@Override
	public void submit(final O task)
	throws InterruptedException {
		INSTANCE.submit(new HttpRequestFuture(task));
	}
	
	@Override
	public int submit(final List<O> tasks, final int from, final int to)
	throws InterruptedException {
		for(int i = from; i < to; i ++) {
			INSTANCE.submit(new HttpRequestFuture(tasks.get(i)));
		}
		return to - from;
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
