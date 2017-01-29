package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.storage.driver.net.base.pool.BasicMultiNodeConnPool;
import com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool;
import com.emc.mongoose.ui.log.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.net.ssl.SslContext;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import static com.emc.mongoose.model.io.task.IoTask.Status.SUCC;
import static com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool.ATTR_KEY_NODE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 30.09.16.
 */
public abstract class NetStorageDriverBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends StorageDriverBase<I, O, R>
implements NetStorageDriver<I, O, R>, ChannelPoolHandler {
	
	private static final Logger LOG = LogManager.getLogger();
	
	protected final String storageNodeAddrs[];
	private final int storageNodePort;
	private final Bootstrap bootstrap;
	private final EventLoopGroup workerGroup;
	private final NonBlockingConnPool connPool;
	private final int socketTimeout;
	private final boolean sslFlag;

	protected NetStorageDriverBase(
		final String jobName, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final SocketConfig socketConfig, final boolean verifyFlag
	) {
		super(jobName, storageConfig.getAuthConfig(), loadConfig, verifyFlag);
		sslFlag = storageConfig.getSsl();
		final long sto = socketConfig.getTimeoutMilliSec();
		if(sto < 0 || sto > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
				"Socket timeout shouldn't be more than " + Integer.MAX_VALUE +
				" seconds and less than 0"
			);
		} else {
			this.socketTimeout = (int) sto;
		}
		final NodeConfig nodeConfig = storageConfig.getNodeConfig();
		storageNodePort = nodeConfig.getPort();
		final String t[] = nodeConfig.getAddrs().toArray(new String[]{});
		storageNodeAddrs = new String[t.length];
		String n;
		for(int i = 0; i < t.length; i ++) {
			n = t[i];
			storageNodeAddrs[i] = n + (n.contains(":") ? "" : ":" + storageNodePort);
		}
		if(SystemUtils.IS_OS_LINUX) {
			workerGroup = new EpollEventLoopGroup(
				Math.min(concurrencyLevel, ThreadUtil.getHardwareConcurrencyLevel()),
				new NamingThreadFactory("ioWorker", true)
			);
		} else {
			workerGroup = new NioEventLoopGroup(
				Math.min(concurrencyLevel, ThreadUtil.getHardwareConcurrencyLevel()),
				new NamingThreadFactory("ioWorker", true)
			);
		}
		bootstrap = new Bootstrap()
			.group(workerGroup)
			.channel(SystemUtils.IS_OS_LINUX ? EpollSocketChannel.class : NioSocketChannel.class );
		//bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator)
		//bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE)
		//bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, )
		//bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR)
		//bootstrap.option(ChannelOption.AUTO_READ)
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, socketConfig.getTimeoutMilliSec());
		int size = (int) socketConfig.getRcvBuf().get();
		if(size > 0) {
			bootstrap.option(ChannelOption.SO_RCVBUF, size);
		}
		size = (int) socketConfig.getSndBuf().get();
		if(size > 0) {
			bootstrap.option(ChannelOption.SO_SNDBUF, size);
		}
		//bootstrap.option(ChannelOption.SO_BACKLOG, socketConfig.getBindBackLogSize());
		bootstrap.option(ChannelOption.SO_KEEPALIVE, socketConfig.getKeepAlive());
		bootstrap.option(ChannelOption.SO_LINGER, socketConfig.getLinger());
		bootstrap.option(ChannelOption.SO_REUSEADDR, socketConfig.getReuseAddr());
		bootstrap.option(ChannelOption.TCP_NODELAY, socketConfig.getTcpNoDelay());
		connPool = new BasicMultiNodeConnPool(
			concurrencyThrottle, concurrencyLevel, storageNodeAddrs, bootstrap, this,
			storageNodePort
		);
		/*for(final String na : storageNodeAddrs) {
			final InetSocketAddress nodeAddr;
			if(na.contains(":")) {
				final String addrParts[] = na.split(":");
				nodeAddr = new InetSocketAddress(addrParts[0], Integer.parseInt(addrParts[1]));
			} else {
				nodeAddr = new InetSocketAddress(na, storageNodePort);
			}
			connPoolMap.put(
				na, new FixedChannelPool(bootstrap.remoteAddress(nodeAddr), this, concurrencyLevel)
			);
		}*/
	}
	
	@Override
	public final void adjustIoBuffers(final SizeInBytes avgDataItemSize, final IoType ioType) {
		int size;
		if(avgDataItemSize.get() < BUFF_SIZE_MIN) {
			size = BUFF_SIZE_MIN;
		} else if(BUFF_SIZE_MAX < avgDataItemSize.get()) {
			size = BUFF_SIZE_MAX;
		} else {
			size = (int) avgDataItemSize.get();
		}
		if(IoType.CREATE.equals(ioType)) {
			LOG.info(
				Markers.MSG, "Adjust output buffer size: {}", SizeInBytes.formatFixedSize(size)
			);
			bootstrap.option(ChannelOption.SO_RCVBUF, BUFF_SIZE_MIN);
			bootstrap.option(ChannelOption.SO_SNDBUF, size);
		} else if(IoType.READ.equals(ioType)) {
			LOG.info(
				Markers.MSG, "Adjust input buffer size: {}", SizeInBytes.formatFixedSize(size)
			);
			bootstrap.option(ChannelOption.SO_RCVBUF, size);
			bootstrap.option(ChannelOption.SO_SNDBUF, BUFF_SIZE_MIN);
		} else {
			bootstrap.option(ChannelOption.SO_RCVBUF, BUFF_SIZE_MIN);
			bootstrap.option(ChannelOption.SO_SNDBUF, BUFF_SIZE_MIN);
		}
	}

	protected Channel getUnpooledConnection()
	throws ConnectException, InterruptedException {

		final String na = storageNodeAddrs[0];
		final InetSocketAddress nodeAddr;
		if(na.contains(":")) {
			final String addrParts[] = na.split(":");
			nodeAddr = new InetSocketAddress(addrParts[0], Integer.parseInt(addrParts[1]));
		} else {
			nodeAddr = new InetSocketAddress(na, storageNodePort);
		}

		final Bootstrap bootstrap = new Bootstrap()
			.group(workerGroup)
			.channel(SystemUtils.IS_OS_LINUX ? EpollSocketChannel.class : NioSocketChannel.class)
			.handler(
				new ChannelInitializer<SocketChannel>() {
					@Override
					protected final void initChannel(final SocketChannel channel)
					throws Exception {
						appendHandlers(channel.pipeline());
					}
				}
			);

		return bootstrap.connect(nodeAddr).sync().channel();
	}
	
	@Override
	protected boolean submit(final O ioTask)
	throws InterruptedException {
		if(!isStarted()) {
			throw new InterruptedException();
		}
		ioTask.reset();
		if(IoType.NOOP.equals(ioTask.getIoType())) {
			concurrencyThrottle.acquire();
			ioTask.startRequest();
			sendRequest(null, ioTask);
			ioTask.finishRequest();
			concurrencyThrottle.release();
			ioTask.setStatus(SUCC);
			complete(null, ioTask);
		} else {
			final Channel conn = connPool.lease();
			if(conn == null) {
				return false;
			}
			conn.attr(ATTR_KEY_IOTASK).set(ioTask);
			ioTask.setNodeAddr(conn.attr(ATTR_KEY_NODE).get());
			ioTask.startRequest();
			sendRequest(conn, ioTask).addListener(new RequestSentCallback(ioTask));
		}
		return true;

	}
	
	@Override @SuppressWarnings("unchecked")
	protected int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		Channel conn;
		O nextIoTask;
		for(int i = from; i < to && isStarted(); i ++) {
			nextIoTask = ioTasks.get(i);
			nextIoTask.reset();
			if(IoType.NOOP.equals(nextIoTask.getIoType())) {
				concurrencyThrottle.acquire();
				nextIoTask.startRequest();
				sendRequest(null, nextIoTask);
				nextIoTask.finishRequest();
				concurrencyThrottle.release();
				nextIoTask.setStatus(SUCC);
				complete(null, nextIoTask);
			} else {
				conn = connPool.lease();
				if(conn == null) {
					return i - from;
				}
				conn.attr(ATTR_KEY_IOTASK).set(nextIoTask);
				nextIoTask.setNodeAddr(conn.attr(ATTR_KEY_NODE).get());
				nextIoTask.startRequest();
				sendRequest(conn, nextIoTask).addListener(new RequestSentCallback(nextIoTask));
			}
		}
		return to - from;
	}
	
	@Override
	protected final int submit(final List<O> ioTasks)
	throws InterruptedException {
		return submit(ioTasks, 0, ioTasks.size());
	}

	protected abstract ChannelFuture sendRequest(final Channel channel, final O ioTask);

	@Override
	public void complete(final Channel channel, final O ioTask) {
		ioTask.finishResponse();
		if(!IoType.NOOP.equals(ioTask.getIoType())) {
			connPool.release(channel);
		}
		ioTaskCompleted(ioTask);
	}

	@Override
	public final void channelReleased(final Channel channel)
	throws Exception {
	}
	
	@Override
	public final void channelAcquired(final Channel channel)
	throws Exception {
	}
	
	@Override
	public final void channelCreated(final Channel channel)
	throws Exception {
		appendHandlers(channel.pipeline());
	}

	protected void appendHandlers(final ChannelPipeline pipeline) {
		if(socketTimeout > 0) {
			pipeline.addLast(
				new IdleStateHandler(
					socketTimeout, socketTimeout, socketTimeout, TimeUnit.MILLISECONDS
				)
			);
		}
		if(sslFlag) {
			final SSLEngine sslEngine = SslContext.INSTANCE.createSSLEngine();
			sslEngine.setUseClientMode(true);
			pipeline.addLast(new SslHandler(sslEngine));
		}
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
	}
	
	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}
	
	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		super.doInterrupt();
		try {
			connPool.close();
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "{}: failed to close the connection pool", toString()
			);
		}
		try {
			if(workerGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS).await(10)) {
				LOG.debug(Markers.MSG, "{}: I/O workers stopped in time", toString());
			} else {
				LOG.debug(Markers.ERR, "{}: I/O workers stopping timeout", toString());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Graceful I/O workers shutdown was interrupted");
		}
	}
}
