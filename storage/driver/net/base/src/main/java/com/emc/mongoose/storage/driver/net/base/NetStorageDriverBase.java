package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.net.ssl.SslContext;
import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.impl.io.UniformOptionSelector;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 30.09.16.
 */
public abstract class NetStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements NetStorageDriver<I, O>, ChannelPoolHandler {
	
	private final static Logger LOG = LogManager.getLogger();
	
	protected final String storageNodeAddrs[];
	protected final int storageNodePort;
	protected final Input<String> nodeSelector;
	protected final Semaphore concurrencyThrottle;
	protected final EventLoopGroup workerGroup;
	protected final Map<String, ChannelPool> connPoolMap = new HashMap<>();
	protected final boolean sslFlag;
	
	protected NetStorageDriverBase(
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final SocketConfig socketConfig, final String srcContainer, final boolean verifyFlag
	) {
		super(runId, storageConfig.getAuthConfig(), loadConfig, srcContainer, verifyFlag);
		sslFlag = storageConfig.getSsl();
		storageNodePort = storageConfig.getPort();
		final String t[] = storageConfig.getNodeConfig().getAddrs().toArray(new String[]{});
		storageNodeAddrs = new String[t.length];
		String n;
		for(int i = 0; i < t.length; i ++) {
			n = t[i];
			storageNodeAddrs[i] = n + (n.contains(":") ? "" : ":" + storageNodePort);
		}
		nodeSelector = new UniformOptionSelector<>(storageNodeAddrs);
		concurrencyThrottle = new Semaphore(concurrencyLevel);
		workerGroup = new NioEventLoopGroup(0, new NamingThreadFactory("ioWorker", true));
		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
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
		for(final String na : storageNodeAddrs) {
			
			final InetSocketAddress nodeAddr;
			if(na.contains(":")) {
				final String addrParts[] = na.split(":");
				nodeAddr = new InetSocketAddress(addrParts[0], Integer.valueOf(addrParts[1]));
			} else {
				nodeAddr = new InetSocketAddress(na, storageNodePort);
			}
			connPoolMap.put(
				na, new FixedChannelPool(bootstrap.remoteAddress(nodeAddr), this, concurrencyLevel)
			);
		}
		
	}
	
	@Override
	public final void put(final O task)
	throws InterruptedIOException {
		final String bestNode;
		if(storageNodeAddrs.length == 1) {
			bestNode = storageNodeAddrs[0];
		} else {
			try {
				bestNode = nodeSelector.get();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to get the best node");
				return;
			}
		}
		if(bestNode == null) {
			return;
		}
		task.setNodeAddr(bestNode);
		try {
			concurrencyThrottle.acquire();
		} catch(final InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
		connPoolMap.get(bestNode).acquire().addListener(getConnectionLeaseCallback(task));
	}
	
	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws IOException {
		final int n = to - from;
		if(storageNodeAddrs.length == 1) {
			O nextTask;
			try {
				for(int i = 0; i < n; i++) {
					nextTask = tasks.get(i + from);
					nextTask.setNodeAddr(storageNodeAddrs[0]);
					concurrencyThrottle.acquire();
					connPoolMap
						.get(storageNodeAddrs[0]).acquire()
						.addListener(getConnectionLeaseCallback(nextTask));
				}
			} catch(final InterruptedException e) {
				throw new InterruptedIOException(e.getMessage());
			}
		} else {
			final List<String> nodeBuff = new ArrayList<>(n);
			if(n != nodeSelector.get(nodeBuff, n)) {
				throw new IllegalStateException("Node selector unexpected behavior");
			}
			O nextTask;
			String nextNode;
			try {
				for(int i = 0; i < n; i++) {
					nextTask = tasks.get(i + from);
					nextNode = nodeBuff.get(i);
					nextTask.setNodeAddr(nextNode);
					concurrencyThrottle.acquire();
					connPoolMap
						.get(nextNode).acquire()
						.addListener(getConnectionLeaseCallback(nextTask));
				}
			} catch(final InterruptedException e) {
				throw new InterruptedIOException(e.getMessage());
			}
		}
		return n;
	}
	
	@Override
	public final int put(final List<O> tasks)
	throws IOException {
		return put(tasks, 0, tasks.size());
	}
	
	protected abstract FutureListener<Channel> getConnectionLeaseCallback(final O ioTask);
	
	@Override
	public final void complete(final Channel channel, final O ioTask) {
		ioTask.finishResponse();
		connPoolMap.get(ioTask.getNodeAddr()).release(channel);
		ioTaskCompleted(ioTask);
	}
	
	@Override
	public final void channelReleased(final Channel channel)
	throws Exception {
		concurrencyThrottle.release();
	}
	
	@Override
	public final void channelAcquired(final Channel channel)
	throws Exception {
	}
	
	@Override
	public void channelCreated(final Channel channel)
	throws Exception {
		final ChannelPipeline pipeline = channel.pipeline();
		if(sslFlag) {
			final SSLEngine sslEngine = SslContext.INSTANCE.createSSLEngine();
			sslEngine.setUseClientMode(true);
			pipeline.addLast(new SslHandler(sslEngine));
		}
	}

	@Override
	public final boolean isIdle() {
		return !concurrencyThrottle.hasQueuedThreads() &&
			concurrencyThrottle.availablePermits() == concurrencyLevel;
	}
	
	@Override
	public final boolean isFullThrottleEntered() {
		// TODO use full load threshold
		return concurrencyThrottle.availablePermits() == 0;
	}
	
	@Override
	public final boolean isFullThrottleExited() {
		// TODO use full load threshold
		return isShutdown() && concurrencyThrottle.availablePermits() > 0;
	}
	
	@Override
	protected void doStart()
	throws IllegalStateException {
	}
	
	@Override
	protected void doShutdown()
	throws IllegalStateException {
	}
	
	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}
	
	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		final Future f = workerGroup.shutdownGracefully(0, 1, TimeUnit.NANOSECONDS);
		try {
			f.await(1, TimeUnit.SECONDS);
		} catch(final InterruptedException e) {
			LOG.warn(Markers.ERR, "Failed to interrupt the HTTP storage driver gracefully");
		}
	}
	
	@Override
	protected void doClose()
	throws IOException {
		super.doClose();
		for(int i = 0; i < storageNodeAddrs.length; i ++) {
			if(!workerGroup.isShutdown()) {
				try {
					connPoolMap.get(storageNodeAddrs[i]).close();
				} catch(final Throwable cause) {
					LogUtil.exception(
						LOG, Level.WARN, cause, "Failed to close the connection pool for {}",
						storageNodeAddrs[i]
					);
				}
			}
			storageNodeAddrs[i] = null;
		}
		connPoolMap.clear();
		nodeSelector.close();
		workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS);
	}
}
