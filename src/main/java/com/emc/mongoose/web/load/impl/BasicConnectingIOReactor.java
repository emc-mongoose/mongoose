package com.emc.mongoose.web.load.impl;
//
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.web.load.impl.tasks.IODispatchTask;
//
import org.apache.http.impl.nio.reactor.BaseIOReactor;
import org.apache.http.impl.nio.reactor.ChannelEntry;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.impl.nio.reactor.SessionRequestHandle;
import org.apache.http.impl.nio.reactor.SessionRequestImpl;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 16.02.15.
 */
public final class BasicConnectingIOReactor
implements ConnectingIOReactor, Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ExecutorService ioExecutor;
	private final IOReactorConfig config;
	private final Queue<SessionRequestImpl> requestQueue  = new ConcurrentLinkedQueue<>();
	private final BaseIOReactor dispatchers[];
	private final Selector selector;
	private final Object statusLock = new Object();
	private final long selectInterval;
	//
	private IOReactorExceptionHandler exceptionHandler;
	private volatile IOReactorStatus status = IOReactorStatus.INACTIVE;
	private long lastTimeoutCheck = System.currentTimeMillis();
	//
	public BasicConnectingIOReactor(
		final IOReactorConfig config, final ThreadFactory threadFactory
	) throws IOReactorException {
		this.config = config;
		ioExecutor = Executors.newFixedThreadPool(
			config.getIoThreadCount(), threadFactory
		);
		dispatchers = new BaseIOReactor[config.getIoThreadCount()];
		try {
			selector = Selector.open();
		} catch(final IOException e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Failed to open the I/O selector");
			throw new IOReactorException("Failed to open the I/O selector");
		}
		selectInterval = config.getSelectInterval();
	}
	@Override
	public final SessionRequest connect(
		final SocketAddress remoteAddress, final SocketAddress localAddress,
		final Object attachment, final SessionRequestCallback callback
	) {
		if(!IOReactorStatus.ACTIVE.equals(status)) {
			throw new IllegalStateException("I/O reactor is not active");
		}
		final SessionRequestImpl sessionRequest = new SessionRequestImpl(
			remoteAddress, localAddress, attachment, callback
		);
		sessionRequest.setConnectTimeout(config.getConnectTimeout());
		//
		requestQueue.add(sessionRequest);
		selector.wakeup();
		//
		return sessionRequest;
	}
	//
	@Override
	public final IOReactorStatus getStatus() {
		return status;
	}
	//
	@Override
	public final void shutdown(final long waitMs)
	throws IOException {
		synchronized(statusLock) {
			if(!IOReactorStatus.ACTIVE.equals(status)) {
				return;
			}
			if(IOReactorStatus.INACTIVE.equals(status)) {
				status = IOReactorStatus.SHUT_DOWN;
				cancelRequests();
				selector.close();
				return;
			}
			status = IOReactorStatus.SHUTDOWN_REQUEST;
		}
		selector.wakeup();
		try {
			ioExecutor.awaitTermination(waitMs, TimeUnit.MILLISECONDS);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Waiting for termination was interrupted");
		} finally {
			LOG.debug(
				Markers.MSG, "Forced shutdown, dropped {} tasks",
				ioExecutor.shutdownNow().size()
			);
		}
	}
	//
	@Override
	public final void shutdown()
	throws IOException {
		shutdown(2000);
	}
	//
	public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	//
	@Override
	public final void execute(final IOEventDispatch ioEventDispatch)
	throws IOException {
		start(ioEventDispatch);
		run();
	}
	//
	private void start(final IOEventDispatch ioEventDispatch)
	throws IOException {
		synchronized(statusLock) {
			if(status.compareTo(IOReactorStatus.SHUTDOWN_REQUEST) >= 0) {
				status = IOReactorStatus.SHUT_DOWN;
				statusLock.notifyAll();
				return;
			}
			if(!IOReactorStatus.INACTIVE.equals(status)) {
				throw new IOException(String.format("Illegal state %s", status));
			}
			status = IOReactorStatus.ACTIVE;
			// start I/O dispatchers
			for(int i = 0; i < config.getIoThreadCount(); i++) {
				final BaseIOReactor dispatcher = new BaseIOReactor(
					selectInterval, config.isInterestOpQueued()
				);
				dispatcher.setExceptionHandler(exceptionHandler);
				ioExecutor.submit(new IODispatchTask(dispatcher, ioEventDispatch));
				dispatchers[i] = dispatcher;
			}
		}
		ioExecutor.shutdown();
	}
	//
	@Override
	public final void run() {
		int readyCount;
		try {
			while(IOReactorStatus.ACTIVE.equals(status)) {
				try {
					readyCount = selector.select(selectInterval);
				} catch(final IOException ex) {
					throw new IOReactorException("Unexpected selector failure", ex);
				}
				//
				processEvents(readyCount);
			}
		} catch(final ClosedSelectorException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Selector closed");
		} catch(final IOReactorException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "I/O reactor failure");
		} finally {
			try {
				doShutdown();
			} catch(final InterruptedIOException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Interrupted I/O");
			}
			synchronized(this.statusLock) {
				this.status = IOReactorStatus.SHUT_DOWN;
				this.statusLock.notifyAll();
			}
		}
	}
	//
	private void processEvents(final int readyCount)
	throws IOReactorException {
		processSessionRequests();
		if(readyCount > 0) {
			final Set<SelectionKey> selectedKeys = selector.selectedKeys();
			for(final SelectionKey key : selectedKeys) {
				processEvent(key);
			}
			selectedKeys.clear();
		}
		final long currentTime = System.currentTimeMillis();
		if((currentTime - lastTimeoutCheck) >= selectInterval) {
			lastTimeoutCheck = currentTime;
			final Set<SelectionKey> keys = this.selector.keys();
			processTimeouts(keys);
		}
	}
	//
	private void doShutdown()
	throws InterruptedIOException {
		synchronized(statusLock) {
			if(status.compareTo(IOReactorStatus.SHUTTING_DOWN) >= 0) {
				return;
			}
			this.status = IOReactorStatus.SHUTTING_DOWN;
		}
		try {
			cancelRequests();
		} catch (final IOReactorException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to cancel the requests");
		}
		this.selector.wakeup();
		// Close out all channels
		if(this.selector.isOpen()) {
			for (final SelectionKey key : selector.keys()) {
				try {
					final Channel channel = key.channel();
					if(channel != null) {
						channel.close();
					}
				} catch (final IOException e) {
					TraceLogger.failure(LOG, Level.WARN, e, "Failed to close the I/O channel");
				}
			}
			// Stop dispatching I/O events
			try {
				selector.close();
			} catch (final IOException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Failed to close the I/O selector");
			}
		}
		// Attempt to shut down I/O dispatchers gracefully
		for(final BaseIOReactor dispatcher : dispatchers) {
			dispatcher.gracefulShutdown();
		}
		//
		final long gracePeriod = config.getShutdownGracePeriod();
		//
		try {
			// Force shut down I/O dispatchers if they fail to terminate in time
			for(final BaseIOReactor dispatcher : dispatchers) {
				if(!IOReactorStatus.INACTIVE.equals(dispatcher.getStatus())) {
					dispatcher.awaitShutdown(gracePeriod);
				}
				if(!IOReactorStatus.SHUT_DOWN.equals(dispatcher.getStatus())) {
					try {
						dispatcher.hardShutdown();
					} catch (final IOReactorException e) {
						TraceLogger.failure(
							LOG, Level.WARN, e, "I/O dispatched hard shutdown failure"
						);
					}
				}
			}
			// Join worker threads
			ioExecutor.shutdownNow();
		} catch (final InterruptedException ex) {
			throw new InterruptedIOException(ex.getMessage());
		}
	}
	//
	private void processEvent(final SelectionKey key) {
		try {
			if(key.isConnectable()) {
				final SocketChannel channel = (SocketChannel) key.channel();
				// Get request handle
				final SessionRequestHandle requestHandle = SessionRequestHandle.class
					.cast(key.attachment());
				final SessionRequestImpl sessionRequest = requestHandle.getSessionRequest();
				// Finish connection process
				try {
					channel.finishConnect();
				} catch(final IOException e) {
					sessionRequest.failed(e);
				}
				key.cancel();
				key.attach(null);
				if(!sessionRequest.isCompleted()) {
					addChannel(new ChannelEntry(channel, sessionRequest));
				} else {
					try {
						channel.close();
					} catch(final IOException e) {
						TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to close the channel");
					}
				}
			}
		} catch(final CancelledKeyException ex) {
			final SessionRequestHandle requestHandle = SessionRequestHandle.class
				.cast(key.attachment());
			key.attach(null);
			if(requestHandle != null) {
				final SessionRequestImpl sessionRequest = requestHandle.getSessionRequest();
				if (sessionRequest != null) {
					sessionRequest.cancel();
				}
			}
		}
	}
	//
	private void processSessionRequests()
	throws IOReactorException {
		SessionRequestImpl request;
		while((request = requestQueue.poll()) != null) {
			if (request.isCompleted()) {
				continue;
			}
			final SocketChannel socketChannel;
			try {
				socketChannel = SocketChannel.open();
			} catch (final IOException ex) {
				throw new IOReactorException("Failure opening socket", ex);
			}
			try {
				validateAddress(request.getLocalAddress());
				validateAddress(request.getRemoteAddress());
				socketChannel.configureBlocking(false);
				prepareSocket(socketChannel.socket());
				if(request.getLocalAddress() != null) {
					final Socket sock = socketChannel.socket();
					sock.setReuseAddress(config.isSoReuseAddress());
					sock.bind(request.getLocalAddress());
				}
				final boolean connected = socketChannel.connect(request.getRemoteAddress());
				if(connected) {
					final ChannelEntry entry = new ChannelEntry(socketChannel, request);
					addChannel(entry);
					continue;
				}
			} catch(final IOException e) {
				closeChannel(socketChannel);
				request.failed(e);
				return;
			}
			//
			final SessionRequestHandle requestHandle = new SessionRequestHandle(request);
			try {
				final SelectionKey key = socketChannel
					.register(selector, SelectionKey.OP_CONNECT, requestHandle);
				//request.setKey(key);
			} catch (final IOException e) {
				closeChannel(socketChannel);
				throw new IOReactorException("Failure registering channel with the selector", e);
			}
		}
	}
	//
	private void processTimeouts(final Set<SelectionKey> keys) {
		final long now = System.currentTimeMillis();
		for(final SelectionKey key : keys) {
			final Object attachment = key.attachment();
			if (attachment instanceof SessionRequestHandle) {
				final SessionRequestHandle handle = (SessionRequestHandle) key.attachment();
				final SessionRequestImpl sessionRequest = handle.getSessionRequest();
				final int timeout = sessionRequest.getConnectTimeout();
				if(timeout > 0) {
					if(handle.getRequestTime() + timeout < now) {
						sessionRequest.timeout();
					}
				}
			}
		}
	}
	//
	private void cancelRequests() throws IOReactorException {
		SessionRequestImpl request;
		while((request = this.requestQueue.poll()) != null) {
			request.cancel();
		}
	}
	//
	private final AtomicInteger countChannels = new AtomicInteger(0);
	//
	private void addChannel(final ChannelEntry entry) {
		dispatchers[countChannels.getAndIncrement() % dispatchers.length]
			.addChannel(entry);
	}
	//
	static void closeChannel(final Channel channel) {
		try {
			channel.close();
		} catch (final IOException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to close the channel");
		}
	}
	//
	private void prepareSocket(final Socket socket)
	throws IOException {
		socket.setTcpNoDelay(this.config.isTcpNoDelay());
		socket.setKeepAlive(this.config.isSoKeepalive());
		if(config.getSoTimeout() > 0) {
			socket.setSoTimeout(this.config.getSoTimeout());
		}
		if(config.getSndBufSize() > 0) {
			socket.setSendBufferSize(this.config.getSndBufSize());
		}
		if(config.getRcvBufSize() > 0) {
			socket.setReceiveBufferSize(this.config.getRcvBufSize());
		}
		final int linger = config.getSoLinger();
		if(linger >= 0) {
			socket.setSoLinger(true, linger);
		}
	}
	//
	private void validateAddress(final SocketAddress address)
	throws UnknownHostException {
		if(address == null) {
			return;
		}
		if(address instanceof InetSocketAddress) {
			final InetSocketAddress endpoint = InetSocketAddress.class.cast(address);
			if(endpoint.isUnresolved()) {
				throw new UnknownHostException(endpoint.getHostName());
			}
		}
	}
}
