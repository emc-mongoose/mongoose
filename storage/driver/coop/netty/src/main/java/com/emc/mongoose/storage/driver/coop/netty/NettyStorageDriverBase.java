package com.emc.mongoose.storage.driver.coop.netty;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.emc.mongoose.base.item.DataItem.rangeCount;
import static com.emc.mongoose.base.item.op.Operation.Status.SUCC;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;
import static com.github.akurilov.netty.connection.pool.NonBlockingConnPool.ATTR_KEY_NODE;

import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.config.IllegalConfigurationException;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.composite.data.CompositeDataOperation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.logging.LogContextThreadFactory;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.storage.driver.coop.CoopStorageDriverBase;
import com.emc.mongoose.storage.driver.coop.netty.data.DataItemFileRegion;
import com.emc.mongoose.storage.driver.coop.netty.data.SeekableByteChannelChunkedNioStream;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.concurrent.ThreadUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.netty.connection.pool.MultiNodeConnPoolImpl;
import com.github.akurilov.netty.connection.pool.NonBlockingConnPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

/** Created by kurila on 30.09.16. */
public abstract class NettyStorageDriverBase<I extends Item, O extends Operation<I>>
				extends CoopStorageDriverBase<I, O> implements NettyStorageDriver<I, O>, ChannelPoolHandler {

	private static final String CLS_NAME = NettyStorageDriverBase.class.getSimpleName();

	private final EventLoopGroup ioExecutor;
	protected final String storageNodeAddrs[];
	protected final Bootstrap bootstrap;
	protected final int storageNodePort;
	protected final int connAttemptsLimit;
	protected final int netTimeoutMilliSec;
	private final Class<SocketChannel> socketChannelCls;
	private final NonBlockingConnPool connPool;
	private final boolean sslFlag;
	protected final ChannelFutureListener reqSentCallback = this::sendFullRequestComplete;

	@SuppressWarnings("unchecked")
	protected NettyStorageDriverBase(
					final String stepId,
					final DataInput itemDataInput,
					final Config storageConfig,
					final boolean verifyFlag,
					final int batchSize)
					throws IllegalConfigurationException, InterruptedException {

		super(stepId, itemDataInput, storageConfig, verifyFlag, batchSize);

		final var netConfig = storageConfig.configVal("net");
		sslFlag = netConfig.boolVal("ssl");
		if (sslFlag) {
			Loggers.MSG.info("{}: SSL/TLS is enabled", stepId);
		}
		final var sto = netConfig.intVal("timeoutMilliSec");
		if (sto > 0) {
			this.netTimeoutMilliSec = sto;
		} else {
			this.netTimeoutMilliSec = Integer.MAX_VALUE;
		}
		final var nodeConfig = netConfig.configVal("node");
		storageNodePort = nodeConfig.intVal("port");
		connAttemptsLimit = nodeConfig.intVal("connAttemptsLimit");
		final String t[] = nodeConfig.<String> listVal("addrs").toArray(new String[]{});
		storageNodeAddrs = new String[t.length];
		String n;
		for (var i = 0; i < t.length; i++) {
			n = t[i];
			storageNodeAddrs[i] = n + (n.contains(":") ? "" : ":" + storageNodePort);
		}

		final int workerCount;
		final var confWorkerCount = storageConfig.intVal("driver-threads");
		if (confWorkerCount < 1) {
			workerCount = ThreadUtil.getHardwareThreadCount();
		} else {
			workerCount = confWorkerCount;
		}

		final Transport transportKey;
		final var transportConfig = netConfig.stringVal("transport");
		if (transportConfig == null || transportConfig.isEmpty()) {
			if (Epoll.isAvailable()) {
				transportKey = Transport.EPOLL;
			} else if (KQueue.isAvailable()) {
				transportKey = Transport.KQUEUE;
			} else {
				transportKey = Transport.NIO;
			}
		} else {
			transportKey = Transport.valueOf(transportConfig.toUpperCase());
		}

		try {

			final var ioExecutorClsName = IO_EXECUTOR_IMPLS.get(transportKey);
			final var transportCls = (Class<EventLoopGroup>) Class.forName(ioExecutorClsName);
			ioExecutor = transportCls
							.getConstructor(Integer.TYPE, ThreadFactory.class)
							.newInstance(workerCount, new LogContextThreadFactory("ioWorker", true));
			Loggers.MSG.info("{}: use {} I/O workers", toString(), workerCount);

			final var ioRatio = netConfig.intVal("ioRatio");
			try {
				final var setIoRatioMethod = transportCls.getMethod("setIoRatio", Integer.TYPE);
				setIoRatioMethod.invoke(ioExecutor, ioRatio);
			} catch (final ReflectiveOperationException e) {
				LogUtil.exception(Level.ERROR, e, "Failed to set the I/O ratio");
			}

		} catch (final ReflectiveOperationException e) {
			throw new AssertionError(e);
		}

		final var socketChannelClsName = SOCKET_CHANNEL_IMPLS.get(transportKey);
		try {
			socketChannelCls = (Class<SocketChannel>) Class.forName(socketChannelClsName);
		} catch (final ReflectiveOperationException e) {
			throw new AssertionError(e);
		}

		bootstrap = new Bootstrap().group(ioExecutor).channel(socketChannelCls);
		// bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator)
		// bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE)
		// bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, )
		// bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR)
		// bootstrap.option(ChannelOption.AUTO_READ)
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, netConfig.intVal("timeoutMilliSec"));
		bootstrap.option(ChannelOption.WRITE_SPIN_COUNT, 1);
		int size = netConfig.intVal("rcvBuf");
		if (size > 0) {
			bootstrap.option(ChannelOption.SO_RCVBUF, size);
		}
		size = netConfig.intVal("sndBuf");
		if (size > 0) {
			bootstrap.option(ChannelOption.SO_SNDBUF, size);
		}
		// bootstrap.option(ChannelOption.SO_BACKLOG, netConfig.getBindBacklogSize());
		bootstrap.option(ChannelOption.SO_KEEPALIVE, netConfig.boolVal("keepAlive"));
		bootstrap.option(ChannelOption.SO_LINGER, netConfig.intVal("linger"));
		bootstrap.option(ChannelOption.SO_REUSEADDR, netConfig.boolVal("reuseAddr"));
		bootstrap.option(ChannelOption.TCP_NODELAY, netConfig.boolVal("tcpNoDelay"));
		try (final var logCtx = CloseableThreadContext.put(KEY_STEP_ID, this.stepId).put(KEY_CLASS_NAME, CLS_NAME)) {
			connPool = createConnectionPool();
		}
	}

	protected NonBlockingConnPool createConnectionPool() {
		return new MultiNodeConnPoolImpl(
						storageNodeAddrs,
						bootstrap,
						this,
						storageNodePort,
						connAttemptsLimit,
						netTimeoutMilliSec,
						TimeUnit.MILLISECONDS);
	}

	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final OpType opType) {
		final int size;
		try (final var logCtx = CloseableThreadContext.put(KEY_STEP_ID, stepId).put(KEY_CLASS_NAME, CLS_NAME)) {
			if (avgTransferSize < BUFF_SIZE_MIN) {
				size = BUFF_SIZE_MIN;
			} else if (BUFF_SIZE_MAX < avgTransferSize) {
				size = BUFF_SIZE_MAX;
			} else {
				size = (int) avgTransferSize;
			}
			if (OpType.CREATE.equals(opType)) {
				Loggers.MSG.info("Adjust output buffer size: {}", SizeInBytes.formatFixedSize(size));
				bootstrap.option(ChannelOption.SO_RCVBUF, BUFF_SIZE_MIN);
				bootstrap.option(ChannelOption.SO_SNDBUF, size);
			} else if (OpType.READ.equals(opType)) {
				Loggers.MSG.info("Adjust input buffer size: {}", SizeInBytes.formatFixedSize(size));
				bootstrap.option(ChannelOption.SO_RCVBUF, size);
				bootstrap.option(ChannelOption.SO_SNDBUF, BUFF_SIZE_MIN);
			} else {
				bootstrap.option(ChannelOption.SO_RCVBUF, BUFF_SIZE_MIN);
				bootstrap.option(ChannelOption.SO_SNDBUF, BUFF_SIZE_MIN);
			}
		}
	}

	protected Channel getUnpooledConnection(final String storageNodeAddr, final int storageNodePort)
					throws ConnectException, InterruptedException {

		final InetSocketAddress socketAddr;
		if (storageNodeAddr.contains(":")) {
			final String addrParts[] = storageNodeAddr.split(":");
			socketAddr = new InetSocketAddress(addrParts[0], Integer.parseInt(addrParts[1]));
		} else {
			socketAddr = new InetSocketAddress(storageNodeAddr, storageNodePort);
		}

		final Bootstrap bootstrap = new Bootstrap()
						.group(ioExecutor)
						.channel(socketChannelCls)
						.handler(
										new ChannelInitializer<SocketChannel>() {
											@Override
											protected final void initChannel(final SocketChannel conn) throws Exception {
												try (final var logCtx = CloseableThreadContext.put(KEY_STEP_ID, stepId)
																.put(KEY_CLASS_NAME, CLS_NAME)) {
													appendHandlers(conn);
													Loggers.MSG.debug(
																	"{}: new unpooled connection {}, pipeline: {}",
																	stepId,
																	conn.hashCode(),
																	conn.pipeline());
												}
											}
										});

		final Channel conn;
		final var connFuture = bootstrap.connect(socketAddr);
		if (netTimeoutMilliSec > 0) {
			if (connFuture.await(netTimeoutMilliSec, TimeUnit.MILLISECONDS)) {
				conn = connFuture.channel();
			} else {
				throw new ConnectTimeoutException();
			}
		} else {
			conn = connFuture.sync().channel();
		}
		return conn;
	}

	@Override
	protected void doStart() throws IllegalStateException {
		super.doStart();
		if (concurrencyLimit > 0) {
			try {
				connPool.preConnect(concurrencyLimit);
			} catch (final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to pre-create the connections");
			} catch (final InterruptedException e) {
				throwUnchecked(e);
			}
		}
	}

	@Override
	protected boolean submit(final O op) throws IllegalStateException {

		ThreadContext.put(KEY_STEP_ID, stepId);
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

		if (!isStarted()) {
			throw new IllegalStateException();
		}
		if (concurrencyThrottle.tryAcquire()) {
			try {
				if (OpType.NOOP.equals(op.type())) {
					op.startRequest();
					sendRequest(null, op);
					op.finishRequest();
					concurrencyThrottle.release();
					op.status(SUCC);
					op.startResponse();
					complete(null, op);
				} else {
					final var conn = connPool.lease();
					if (conn == null) {
						return false;
					}
					conn.attr(ATTR_KEY_OPERATION).set(op);
					op.nodeAddr(conn.attr(ATTR_KEY_NODE).get());
					op.startRequest();
					sendRequest(conn, op);
				}
			} catch (final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to lease the connection for the load operation");
				op.status(Operation.Status.FAIL_IO);
				complete(null, op);
			} catch (final Throwable thrown) {
				throwUncheckedIfInterrupted(thrown);
				LogUtil.exception(Level.WARN, thrown, "Failed to submit the load operation");
				op.status(Operation.Status.FAIL_UNKNOWN);
				complete(null, op);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected int submit(final List<O> ops, final int from, final int to)
					throws IllegalStateException {

		if (ops.size() == 0) {
			return 0;
		}
		final var needed = to - from;
		if (needed == 0) {
			return 0;
		}
		var permits = concurrencyThrottle.drainPermits();
		if (permits == 0) {
			return 0;
		}
		if (permits > needed) {
			concurrencyThrottle.release(permits - needed);
			permits = needed;
		}

		ThreadContext.put(KEY_STEP_ID, stepId);
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

		Channel conn;
		O nextOp = null;
		var n = 0;
		try {
			while (n < permits && isStarted()) {
				nextOp = ops.get(from + n);
				if (OpType.NOOP.equals(nextOp.type())) {
					nextOp.startRequest();
					sendRequest(null, nextOp);
					nextOp.finishRequest();
					concurrencyThrottle.release();
					nextOp.status(SUCC);
					nextOp.startResponse();
					complete(null, nextOp);
				} else {
					conn = connPool.lease();
					if (conn == null) {
						return n;
					}
					conn.attr(ATTR_KEY_OPERATION).set(nextOp);
					nextOp.nodeAddr(conn.attr(ATTR_KEY_NODE).get());
					nextOp.startRequest();
					sendRequest(conn, nextOp);
				}
				n++;
			}
		} catch (final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to lease the connection for the load operation");
			nextOp.status(Operation.Status.FAIL_IO);
			complete(null, nextOp);
			if (permits - n > 1) {
				concurrencyThrottle.release(permits - n - 1);
			}
		} catch (final Throwable thrown) {
			throwUncheckedIfInterrupted(thrown);
			LogUtil.exception(Level.WARN, thrown, "Failed to submit the load operations");
			nextOp.status(Operation.Status.FAIL_UNKNOWN);
			complete(null, nextOp);
			if (permits - n > 1) {
				concurrencyThrottle.release(permits - n - 1);
			}
		}
		return n;
	}

	@Override
	protected final int submit(final List<O> ops)
					throws IllegalStateException {
		return submit(ops, 0, ops.size());
	}

	/**
	* Note that the particular implementation should also invoke the {@link #sendRequestData(Channel,
	* Operation)} method to send the actual payload (if any).
	*
	* @param channel the channel to send request to
	* @param op the load operation describing the item and the operation type to perform
	*/
	protected abstract void sendRequest(final Channel channel, final O op);

	protected final void sendRequestData(final Channel channel, final O op) throws IOException {

		final var opType = op.type();

		if (OpType.CREATE.equals(opType)) {
			final var item = op.item();
			if (item instanceof DataItem) {
				final var dataOp = (DataOperation) op;
				if (!(dataOp instanceof CompositeDataOperation)) {
					final var dataItem = (DataItem) item;
					final var srcPath = dataOp.srcPath();
					if (0 < dataItem.size() && (null == srcPath || srcPath.isEmpty())) {
						if (sslFlag) {
							channel.write(new SeekableByteChannelChunkedNioStream(dataItem));
						} else {
							channel.write(new DataItemFileRegion(dataItem));
						}
					}
					dataOp.countBytesDone(dataItem.size());
				}
			}
		} else if (OpType.UPDATE.equals(opType)) {
			final var item = op.item();
			if (item instanceof DataItem) {

				final var dataItem = (DataItem) item;
				final var dataOp = (DataOperation) op;

				final var fixedRanges = (List<Range>) dataOp.fixedRanges();
				if (fixedRanges == null || fixedRanges.isEmpty()) {
					// random ranges update case
					final var updRangesMaskPair = dataOp.markedRangesMaskPair();
					final var rangeCount = rangeCount(dataItem.size());
					DataItem updatedRange;
					if (sslFlag) {
						// current layer updates first
						for (var i = 0; i < rangeCount; i++) {
							if (updRangesMaskPair[0].get(i)) {
								dataOp.currRangeIdx(i);
								updatedRange = dataOp.currRangeUpdate();
								channel.write(new SeekableByteChannelChunkedNioStream(updatedRange));
							}
						}
						// then next layer updates if any
						for (var i = 0; i < rangeCount; i++) {
							if (updRangesMaskPair[1].get(i)) {
								dataOp.currRangeIdx(i);
								updatedRange = dataOp.currRangeUpdate();
								channel.write(new SeekableByteChannelChunkedNioStream(updatedRange));
							}
						}
					} else {
						// current layer updates first
						for (var i = 0; i < rangeCount; i++) {
							if (updRangesMaskPair[0].get(i)) {
								dataOp.currRangeIdx(i);
								updatedRange = dataOp.currRangeUpdate();
								channel.write(new DataItemFileRegion(updatedRange));
							}
						}
						// then next layer updates if any
						for (var i = 0; i < rangeCount; i++) {
							if (updRangesMaskPair[1].get(i)) {
								dataOp.currRangeIdx(i);
								updatedRange = dataOp.currRangeUpdate();
								channel.write(new DataItemFileRegion(updatedRange));
							}
						}
					}
					dataItem.commitUpdatedRanges(dataOp.markedRangesMaskPair());
				} else { // fixed byte ranges case
					final var baseItemSize = dataItem.size();
					long beg;
					long end;
					long size;
					if (sslFlag) {
						for (final var fixedRange : fixedRanges) {
							beg = fixedRange.getBeg();
							end = fixedRange.getEnd();
							size = fixedRange.getSize();
							if (size == -1) {
								if (beg == -1) {
									beg = baseItemSize - end;
									size = end;
								} else if (end == -1) {
									size = baseItemSize - beg;
								} else {
									size = end - beg + 1;
								}
							} else {
								// append
								beg = baseItemSize;
								// note down the new size
								dataItem.size(dataItem.size() + dataOp.markedRangesSize());
							}
							channel.write(new SeekableByteChannelChunkedNioStream(dataItem.slice(beg, size)));
						}
					} else {
						for (final var fixedRange : fixedRanges) {
							beg = fixedRange.getBeg();
							end = fixedRange.getEnd();
							size = fixedRange.getSize();
							if (size == -1) {
								if (beg == -1) {
									beg = baseItemSize - end;
									size = end;
								} else if (end == -1) {
									size = baseItemSize - beg;
								} else {
									size = end - beg + 1;
								}
							} else {
								// append
								beg = baseItemSize;
								// note down the new size
								dataItem.size(dataItem.size() + dataOp.markedRangesSize());
							}
							channel.write(new DataItemFileRegion(dataItem.slice(beg, size)));
						}
					}
				}
				dataOp.countBytesDone(dataOp.markedRangesSize());
			}
		}
	}

	void sendFullRequestComplete(final ChannelFuture future) {
		final var op = future.channel().attr(ATTR_KEY_OPERATION).get();
		try {
			op.finishRequest();
		} catch (final IllegalStateException e) {
			LogUtil.exception(Level.DEBUG, e, "{}", op.toString());
		}
	}

	@Override
	public void complete(final Channel channel, final O op) {

		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);
		ThreadContext.put(KEY_STEP_ID, stepId);

		try {
			op.finishResponse();
		} catch (final IllegalStateException e) {
			LogUtil.exception(Level.DEBUG, e, "{}: invalid load operation state", op.toString());
		}
		concurrencyThrottle.release();
		if (channel != null) {
			connPool.release(channel);
		}
		handleCompleted(op);
	}

	@Override
	public final void channelReleased(final Channel channel) throws Exception {}

	@Override
	public final void channelAcquired(final Channel channel) throws Exception {}

	@Override
	public final void channelCreated(final Channel channel) throws Exception {
		try (final var ctx = CloseableThreadContext.put(KEY_STEP_ID, stepId).put(KEY_CLASS_NAME, CLS_NAME)) {
			appendHandlers(channel);
			if (Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace(
								"{}: new channel pipeline configured: {}", stepId, channel.pipeline().toString());
			}
		}
	}

	protected void appendHandlers(final Channel channel) {
		final var pipeline = channel.pipeline();
		if (sslFlag) {
			Loggers.MSG.debug("{}: SSL/TLS is enabled for the channel", stepId);
			pipeline.addLast(SslUtil.CLIENT_SSL_CONTEXT.newHandler(channel.alloc()));
		}
		if (netTimeoutMilliSec > 0) {
			pipeline.addLast(
							new IdleStateHandler(
											netTimeoutMilliSec, netTimeoutMilliSec, netTimeoutMilliSec, TimeUnit.MILLISECONDS));
		}
	}

	@Override
	protected final void doStop() throws IllegalStateException {
		try (final var ctx = CloseableThreadContext.put(KEY_STEP_ID, stepId).put(KEY_CLASS_NAME, CLS_NAME)) {
			try {
				Loggers.MSG.debug("{}: shutdown the I/O executor", toString());
				if (ioExecutor
								.shutdownGracefully(0, 0, TimeUnit.NANOSECONDS)
								.await(1, TimeUnit.MICROSECONDS)) {
					Loggers.MSG.debug("{}: I/O workers stopped in time", toString());
				} else {
					Loggers.ERR.debug("{}: I/O workers stopping timeout", toString());
				}
			} catch (final InterruptedException e) {
				LogUtil.exception(Level.WARN, e, "Graceful I/O workers shutdown was interrupted");
				throwUnchecked(e);
			}
		}
	}

	@Override
	protected void doClose() throws IllegalStateException, IOException {
		try {
			connPool.close();
		} catch (final IOException e) {
			LogUtil.exception(Level.WARN, e, "{}: failed to close the connection pool", toString());
		}
		super.doClose();
	}
}
