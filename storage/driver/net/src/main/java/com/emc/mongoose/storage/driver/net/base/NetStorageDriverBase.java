package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.api.common.ByteRange;
import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.concurrent.StoppableTask;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.storage.driver.net.base.data.DataItemFileRegion;
import com.emc.mongoose.storage.driver.net.base.data.SeekableByteChannelChunkedNioStream;
import com.emc.mongoose.storage.driver.net.base.pool.BasicMultiNodeConnPool;
import com.emc.mongoose.storage.driver.net.base.pool.ConnLeaseException;
import com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool;
import com.emc.mongoose.api.model.NamingThreadFactory;
import com.emc.mongoose.api.common.concurrent.ThreadUtil;
import com.emc.mongoose.api.common.net.ssl.SslContext;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.model.io.task.IoTask.Status.SUCC;
import static com.emc.mongoose.api.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool.ATTR_KEY_NODE;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.net.NetConfig;
import com.emc.mongoose.ui.config.storage.net.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 30.09.16.
 */
public abstract class NetStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements NetStorageDriver<I, O>, ChannelPoolHandler {

	private static final String CLS_NAME = NetStorageDriverBase.class.getSimpleName();

	private static final Lock IO_EXECUTOR_LOCK = new ReentrantLock();
	private static EventLoopGroup IO_EXECUTOR = null;
	private static int IO_EXECUTOR_REF_COUNT = 0;

	protected final String storageNodeAddrs[];
	protected final Bootstrap bootstrap;
	protected final int storageNodePort;
	protected final int connAttemptsLimit;
	private final Class<SocketChannel> socketChannelCls;
	private final NonBlockingConnPool connPool;
	private final int socketTimeout;
	private final boolean sslFlag;

	@SuppressWarnings("unchecked")
	protected NetStorageDriverBase(
		final String jobName, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException, InterruptedException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
		final NetConfig netConfig = storageConfig.getNetConfig();
		sslFlag = netConfig.getSsl();
		final long sto = netConfig.getTimeoutMilliSec();
		if(sto < 0 || sto > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
				"Socket timeout shouldn't be more than " + Integer.MAX_VALUE +
				" seconds and less than 0"
			);
		} else {
			this.socketTimeout = (int) sto;
		}
		final NodeConfig nodeConfig = netConfig.getNodeConfig();
		storageNodePort = nodeConfig.getPort();
		connAttemptsLimit = nodeConfig.getConnAttemptsLimit();
		final String t[] = nodeConfig.getAddrs().toArray(new String[]{});
		storageNodeAddrs = new String[t.length];
		String n;
		for(int i = 0; i < t.length; i ++) {
			n = t[i];
			storageNodeAddrs[i] = n + (n.contains(":") ? "" : ":" + storageNodePort);
		}
		
		final int workerCount;
		final int confWorkerCount = storageConfig.getDriverConfig().getThreads();
		if(confWorkerCount < 1) {
			workerCount = ThreadUtil.getHardwareThreadCount();
		} else {
			workerCount = confWorkerCount;
		}
		final int ioRatio = netConfig.getIoRatio();
		final Transport transportKey = Transport.valueOf(netConfig.getTransport().toUpperCase());

		if(IO_EXECUTOR_LOCK.tryLock(StoppableTask.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			try {
				if(IO_EXECUTOR == null) {
					Loggers.MSG.info("{}: I/O executor doesn't exist yet", toString());
					if(IO_EXECUTOR_REF_COUNT != 0) {
						throw new AssertionError("I/O executor reference count should be 0");
					}
					try {
						final String ioExecutorClsName = IO_EXECUTOR_IMPLS.get(transportKey);
						final Class<EventLoopGroup> transportCls = (Class<EventLoopGroup>) Class
							.forName(ioExecutorClsName);
						IO_EXECUTOR = transportCls
							.getConstructor(Integer.TYPE, ThreadFactory.class)
							.newInstance(workerCount, new NamingThreadFactory("ioWorker", true));
						try {
							final Method setIoRatioMethod = transportCls.getMethod(
								"setIoRatio", Integer.TYPE
							);
							setIoRatioMethod.invoke(IO_EXECUTOR, ioRatio);
						} catch(final ReflectiveOperationException e) {
							LogUtil.exception(Level.ERROR, e, "Failed to set the I/O ratio");
						}
					} catch(final ReflectiveOperationException e) {
						throw new AssertionError(e);
					}
				}
				IO_EXECUTOR_REF_COUNT ++;
				Loggers.MSG.debug(
					"{}: increased the I/O executor ref count to {}", toString(),
					IO_EXECUTOR_REF_COUNT
				);
			} finally {
				IO_EXECUTOR_LOCK.unlock();
			}
		} else {
			Loggers.ERR.error("Failed to obtain the I/O executor lock in time");
		}

		final String socketChannelClsName = SOCKET_CHANNEL_IMPLS.get(transportKey);
		try {
			socketChannelCls = (Class<SocketChannel>) Class.forName(socketChannelClsName);
		} catch(final ReflectiveOperationException e) {
			throw new AssertionError(e);
		}

		bootstrap = new Bootstrap()
			.group(IO_EXECUTOR)
			.channel(socketChannelCls);
		//bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator)
		//bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE)
		//bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, )
		//bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR)
		//bootstrap.option(ChannelOption.AUTO_READ)
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, netConfig.getTimeoutMilliSec());
		//bootstrap.option(ChannelOption.WRITE_SPIN_COUNT, 32);
		int size = (int) netConfig.getRcvBuf().get();
		if(size > 0) {
			bootstrap.option(ChannelOption.SO_RCVBUF, size);
		}
		size = (int) netConfig.getSndBuf().get();
		if(size > 0) {
			bootstrap.option(ChannelOption.SO_SNDBUF, size);
		}
		//bootstrap.option(ChannelOption.SO_BACKLOG, netConfig.getBindBacklogSize());
		bootstrap.option(ChannelOption.SO_KEEPALIVE, netConfig.getKeepAlive());
		bootstrap.option(ChannelOption.SO_LINGER, netConfig.getLinger());
		bootstrap.option(ChannelOption.SO_REUSEADDR, netConfig.getReuseAddr());
		bootstrap.option(ChannelOption.TCP_NODELAY, netConfig.getTcpNoDelay());
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			connPool = createConnectionPool();
		}
	}

	protected NonBlockingConnPool createConnectionPool() {
		return new BasicMultiNodeConnPool(
			concurrencyLevel, concurrencyThrottle, storageNodeAddrs, bootstrap, this,
			storageNodePort, connAttemptsLimit
		);
	}
	
	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final IoType ioType) {
		int size;
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			if(avgTransferSize < BUFF_SIZE_MIN) {
				size = BUFF_SIZE_MIN;
			} else if(BUFF_SIZE_MAX < avgTransferSize) {
				size = BUFF_SIZE_MAX;
			} else {
				size = (int) avgTransferSize;
			}
			if(IoType.CREATE.equals(ioType)) {
				Loggers.MSG.info(
					"Adjust output buffer size: {}", SizeInBytes.formatFixedSize(size)
				);
				bootstrap.option(ChannelOption.SO_RCVBUF, BUFF_SIZE_MIN);
				bootstrap.option(ChannelOption.SO_SNDBUF, size);
			} else if(IoType.READ.equals(ioType)) {
				Loggers.MSG.info("Adjust input buffer size: {}", SizeInBytes.formatFixedSize(size));
				bootstrap.option(ChannelOption.SO_RCVBUF, size);
				bootstrap.option(ChannelOption.SO_SNDBUF, BUFF_SIZE_MIN);
			} else {
				bootstrap.option(ChannelOption.SO_RCVBUF, BUFF_SIZE_MIN);
				bootstrap.option(ChannelOption.SO_SNDBUF, BUFF_SIZE_MIN);
			}
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
			.group(IO_EXECUTOR)
			.channel(socketChannelCls)
			.handler(
				new ChannelInitializer<SocketChannel>() {
					@Override
					protected final void initChannel(final SocketChannel channel)
					throws Exception {
						try(
							final Instance logCtx = CloseableThreadContext
								.put(KEY_TEST_STEP_ID, stepId)
								.put(KEY_CLASS_NAME, CLS_NAME)
						) {
							appendHandlers(channel.pipeline());
							Loggers.MSG.debug(
								"{}: new unpooled channel {}, pipeline: {}", stepId,
								channel.hashCode(), channel.pipeline()
							);
						}
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
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			if(IoType.NOOP.equals(ioTask.getIoType())) {
				concurrencyThrottle.acquire();
				ioTask.startRequest();
				sendRequest(null, null, ioTask);
				ioTask.finishRequest();
				concurrencyThrottle.release();
				ioTask.setStatus(SUCC);
				ioTask.startResponse();
				complete(null, ioTask);
			} else {
				final Channel conn = connPool.lease();
				if(conn == null) {
					return false;
				}
				conn.attr(ATTR_KEY_IOTASK).set(ioTask);
				ioTask.setNodeAddr(conn.attr(ATTR_KEY_NODE).get());
				ioTask.startRequest();
				sendRequest(
					conn, conn.newPromise().addListener(new RequestSentCallback(ioTask)), ioTask
				);
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.WARN, e, "Submit the I/O task in the invalid state");
		} catch(final ConnLeaseException e) {
			LogUtil.exception(Level.WARN, e, "Failed to lease the connection for the I/O task");
			ioTask.setStatus(IoTask.Status.FAIL_IO);
			complete(null, ioTask);
		}
		return true;

	}
	
	@Override @SuppressWarnings("unchecked")
	protected int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		Channel conn;
		O nextIoTask;
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			for(int i = from; i < to && isStarted(); i ++) {
				nextIoTask = ioTasks.get(i);
				if(IoType.NOOP.equals(nextIoTask.getIoType())) {
					concurrencyThrottle.acquire();
					nextIoTask.startRequest();
					sendRequest(null, null, nextIoTask);
					nextIoTask.finishRequest();
					concurrencyThrottle.release();
					nextIoTask.setStatus(SUCC);
					nextIoTask.startResponse();
					complete(null, nextIoTask);
				} else {
					conn = connPool.lease();
					if(conn == null) {
						return i - from;
					}
					conn.attr(ATTR_KEY_IOTASK).set(nextIoTask);
					nextIoTask.setNodeAddr(conn.attr(ATTR_KEY_NODE).get());
					nextIoTask.startRequest();
					sendRequest(
						conn, conn.newPromise().addListener(new RequestSentCallback(nextIoTask)),
						nextIoTask
					);
				}
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.WARN, e, "Submit the I/O task in the invalid state");
		} catch(final RejectedExecutionException e) {
			if(!isInterrupted()) {
				LogUtil.exception(Level.WARN, e, "Failed to submit the I/O task");
			}
		} catch(final ConnLeaseException e) {
			LogUtil.exception(Level.WARN, e, "Failed to lease the connection for the I/O task");
			for(int i = from; i < to; i ++) {
				nextIoTask = ioTasks.get(i);
				nextIoTask.setStatus(IoTask.Status.FAIL_IO);
				complete(null, nextIoTask);
			}
		}
		return to - from;
	}
	
	@Override
	protected final int submit(final List<O> ioTasks)
	throws InterruptedException {
		return submit(ioTasks, 0, ioTasks.size());
	}
	
	/**
	 Note that the particular implementation should also invoke
	 the {@link #sendRequestData(Channel, IoTask)} method to send the actual payload (if any).
	 @param channel the channel to send request to
	 @param channelPromise the promise which will be invoked when the request is sent completely
	 @param ioTask the I/O task describing the item and the operation to perform
	 */
	protected abstract void sendRequest(
		final Channel channel, final ChannelPromise channelPromise, final O ioTask
	);
	
	protected final void sendRequestData(final Channel channel, final O ioTask)
	throws IOException {
		
		final IoType ioType = ioTask.getIoType();
		
		if(IoType.CREATE.equals(ioType)) {
			final I item = ioTask.getItem();
			if(item instanceof DataItem) {
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				if(!(dataIoTask instanceof CompositeDataIoTask)) {
					final DataItem dataItem = (DataItem) item;
					final String srcPath = dataIoTask.getSrcPath();
					if(null == srcPath || srcPath.isEmpty()) {
						if(sslFlag) {
							channel.write(new SeekableByteChannelChunkedNioStream(dataItem));
						} else {
							channel.write(new DataItemFileRegion(dataItem));
						}
					}
					dataIoTask.setCountBytesDone(dataItem.size());
				}
			}
		} else if(IoType.UPDATE.equals(ioType)) {
			final I item = ioTask.getItem();
			if(item instanceof DataItem) {
				
				final DataItem dataItem = (DataItem) item;
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				
				final List<ByteRange> fixedByteRanges = dataIoTask.getFixedRanges();
				if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
					// random ranges update case
					final BitSet updRangesMaskPair[] = dataIoTask.getMarkedRangesMaskPair();
					final int rangeCount = getRangeCount(dataItem.size());
					DataItem updatedRange;
					if(sslFlag) {
						// current layer updates first
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[0].get(i)) {
								dataIoTask.setCurrRangeIdx(i);
								updatedRange = dataIoTask.getCurrRangeUpdate();
								channel.write(
									new SeekableByteChannelChunkedNioStream(updatedRange)
								);
							}
						}
						// then next layer updates if any
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[1].get(i)) {
								dataIoTask.setCurrRangeIdx(i);
								updatedRange = dataIoTask.getCurrRangeUpdate();
								channel.write(
									new SeekableByteChannelChunkedNioStream(updatedRange)
								);
							}
						}
					} else {
						// current layer updates first
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[0].get(i)) {
								dataIoTask.setCurrRangeIdx(i);
								updatedRange = dataIoTask.getCurrRangeUpdate();
								channel.write(new DataItemFileRegion(updatedRange));
							}
						}
						// then next layer updates if any
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[1].get(i)) {
								dataIoTask.setCurrRangeIdx(i);
								updatedRange = dataIoTask.getCurrRangeUpdate();
								channel.write(new DataItemFileRegion(updatedRange));
							}
						}
					}
					dataItem.commitUpdatedRanges(dataIoTask.getMarkedRangesMaskPair());
				} else { // fixed byte ranges case
					final long baseItemSize = dataItem.size();
					long beg;
					long end;
					long size;
					if(sslFlag) {
						for(final ByteRange fixedByteRange : fixedByteRanges) {
							beg = fixedByteRange.getBeg();
							end = fixedByteRange.getEnd();
							size = fixedByteRange.getSize();
							if(size == -1) {
								if(beg == -1) {
									beg = baseItemSize - end;
									size = end;
								} else if(end == -1) {
									size = baseItemSize - beg;
								} else {
									size = end - beg + 1;
								}
							} else {
								// append
								beg = baseItemSize;
								// note down the new size
								dataItem.size(
									dataItem.size() + dataIoTask.getMarkedRangesSize()
								);
							}
							channel.write(
								new SeekableByteChannelChunkedNioStream(
									dataItem.slice(beg, size)
								)
							);
						}
					} else {
						for(final ByteRange fixedByteRange : fixedByteRanges) {
							beg = fixedByteRange.getBeg();
							end = fixedByteRange.getEnd();
							size = fixedByteRange.getSize();
							if(size == -1) {
								if(beg == -1) {
									beg = baseItemSize - end;
									size = end;
								} else if(end == -1) {
									size = baseItemSize - beg;
								} else {
									size = end - beg + 1;
								}
							} else {
								// append
								beg = baseItemSize;
								// note down the new size
								dataItem.size(
									dataItem.size() + dataIoTask.getMarkedRangesSize()
								);
							}
							channel.write(new DataItemFileRegion(dataItem.slice(beg, size)));
						}
					}
				}
				dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
			}
		}
	}

	@Override
	public void complete(final Channel channel, final O ioTask) {
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			ioTask.finishResponse();
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.DEBUG, e, "{}: invalid I/O task state", ioTask.toString());
		}
		if(channel != null) {
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
		final ChannelPipeline pipeline = channel.pipeline();
		appendHandlers(pipeline);
		if(Loggers.MSG.isTraceEnabled()) {
			Loggers.MSG.trace("{}: new channel pipeline configured: {}", stepId, pipeline.toString());
		}
	}

	protected void appendHandlers(final ChannelPipeline pipeline) {
		if(sslFlag) {
			Loggers.MSG.debug("{}: SSL/TLS is enabled for the channel", stepId);
			final SSLEngine sslEngine = SslContext.INSTANCE.createSSLEngine();
			sslEngine.setEnabledProtocols(
				new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3" }
			);
			sslEngine.setUseClientMode(true);
			sslEngine.setEnabledCipherSuites(
				SslContext.INSTANCE.getServerSocketFactory().getSupportedCipherSuites()
			);
			pipeline.addLast(new SslHandler(sslEngine));
		}
		if(socketTimeout > 0) {
			pipeline.addLast(
				new IdleStateHandler(
					socketTimeout, socketTimeout, socketTimeout, TimeUnit.MILLISECONDS
				)
			);
		}
	}
	
	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			super.doInterrupt();
			try {
				connPool.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: failed to close the connection pool", toString()
				);
			}
			try {
				if(IO_EXECUTOR_LOCK.tryLock(StoppableTask.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
					try {
						IO_EXECUTOR_REF_COUNT --;
						Loggers.MSG.debug(
							"{}: decreased the I/O executor ref count to {}", toString(),
							IO_EXECUTOR_REF_COUNT
						);
						if(IO_EXECUTOR_REF_COUNT == 0) {
							Loggers.MSG.info("{}: shutdown the I/O executor", toString());
							if(IO_EXECUTOR.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS).await(10)) {
								Loggers.MSG.debug("{}: I/O workers stopped in time", toString());
							} else {
								Loggers.ERR.debug("{}: I/O workers stopping timeout", toString());
							}
							IO_EXECUTOR = null;
						}
					} finally {
						IO_EXECUTOR_LOCK.unlock();
					}
				} else {
					Loggers.ERR.error("Failed to obtain the I/O executor lock in time");
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(Level.WARN, e, "Graceful I/O workers shutdown was interrupted");
			}
		}
	}
}
