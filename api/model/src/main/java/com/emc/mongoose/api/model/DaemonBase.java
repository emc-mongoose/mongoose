package com.emc.mongoose.api.model;

import com.emc.mongoose.api.common.concurrent.Coroutine;
import com.emc.mongoose.api.common.concurrent.Daemon;
import com.emc.mongoose.api.common.concurrent.StoppableTask;
import com.emc.mongoose.api.model.svc.SvcWorkerTask;

import static com.emc.mongoose.api.common.concurrent.Daemon.State.CLOSED;
import static com.emc.mongoose.api.common.concurrent.Daemon.State.INITIAL;
import static com.emc.mongoose.api.common.concurrent.Daemon.State.INTERRUPTED;
import static com.emc.mongoose.api.common.concurrent.Daemon.State.SHUTDOWN;
import static com.emc.mongoose.api.common.concurrent.Daemon.State.STARTED;
import static com.emc.mongoose.api.common.concurrent.ThreadUtil.getHardwareThreadCount;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class DaemonBase
implements Daemon {

	private static final ThreadPoolExecutor SVC_EXECUTOR;
	private static final List<StoppableTask> SVC_WORKERS = new ArrayList<>();
	private static final Map<Daemon, List<Coroutine>> SVC_COROUTINES = new ConcurrentHashMap<>();

	static {

		final int svcThreadCount = getHardwareThreadCount();

		SVC_EXECUTOR = new ThreadPoolExecutor(
			svcThreadCount, svcThreadCount, 0, TimeUnit.DAYS, new ArrayBlockingQueue<>(1),
			new NamingThreadFactory("svcWorker", true)
		);

		for(int i = 0; i < getHardwareThreadCount(); i ++) {
			final StoppableTask svcWorkerTask = new SvcWorkerTask(SVC_COROUTINES);
			SVC_EXECUTOR.submit(svcWorkerTask);
			SVC_WORKERS.add(svcWorkerTask);
		}
	}

	public static void setThreadCount(final int threadCount) {
		final int newThreadCount = threadCount > 0 ? threadCount : getHardwareThreadCount();
		final int oldThreadCount = SVC_EXECUTOR.getCorePoolSize();
		if(newThreadCount != oldThreadCount) {
			SVC_EXECUTOR.setCorePoolSize(newThreadCount);
			SVC_EXECUTOR.setMaximumPoolSize(newThreadCount);
			if(newThreadCount > oldThreadCount) {
				for(int i = oldThreadCount; i < newThreadCount; i ++) {
					final SvcWorkerTask svcWorkerTask = new SvcWorkerTask(SVC_COROUTINES);
					SVC_EXECUTOR.submit(svcWorkerTask);
					SVC_WORKERS.add(svcWorkerTask);
				}
			} else { // less, remove some active service worker tasks
				try {
					for(int i = oldThreadCount - 1; i >= newThreadCount; i --) {
						SVC_WORKERS.remove(i).close();
					}
				} catch (final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}
	
	protected final List<Coroutine> svcCoroutines = new CopyOnWriteArrayList<>();
	
	private AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
	protected final Object state = new Object();
	
	@Override
	public final State getState() {
		return stateRef.get();
	}
	
	protected void doStart()
	throws IllegalStateException {
		SVC_COROUTINES.put(this, svcCoroutines);
	}

	protected abstract void doShutdown()
	throws IllegalStateException;

	protected abstract void doInterrupt()
	throws IllegalStateException;
	
	protected void doClose()
	throws IOException, IllegalStateException {
		SVC_COROUTINES.remove(this);
		for(final Coroutine svcCoroutine : svcCoroutines) {
			svcCoroutine.close();
		}
		svcCoroutines.clear();
	}

	@Override
	public final List<Coroutine> getSvcCoroutines() {
		return svcCoroutines;
	}

	@Override
	public final void start()
	throws IllegalStateException {
		if(stateRef.compareAndSet(INITIAL, STARTED)) {
			synchronized(state) {
				state.notifyAll();
			}
			doStart();
		} else {
			throw new IllegalStateException("start failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isStarted() {
		return stateRef.get().equals(STARTED);
	}

	@Override
	public final synchronized void shutdown()
	throws IllegalStateException {
		if(stateRef.compareAndSet(INITIAL, SHUTDOWN) || stateRef.compareAndSet(STARTED, SHUTDOWN)) {
			synchronized(state) {
				state.notifyAll();
			}
			doShutdown();
		} else {
			throw new IllegalStateException("shutdown failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isShutdown() {
		return stateRef.get().equals(SHUTDOWN);
	}
	
	@Override
	public final void await()
	throws InterruptedException, RemoteException {
		await(Long.MAX_VALUE, TimeUnit.SECONDS);
	}
	
	@Override
	public final synchronized void interrupt()
	throws IllegalStateException {
		try {
			shutdown();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(SHUTDOWN, INTERRUPTED)) {
			synchronized(state) {
				state.notifyAll();
			}
			doInterrupt();
		} else {
			throw new IllegalStateException("interrupt failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isInterrupted() {
		return stateRef.get().equals(INTERRUPTED);
	}
	
	@Override
	public final synchronized void close()
	throws IOException, IllegalStateException {
		try {
			interrupt();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(INTERRUPTED, CLOSED)) {
			synchronized(state) {
				state.notifyAll();
			}
			doClose();
			// may be closed by another thread right after the interruption
		} else if(!CLOSED.equals(stateRef.get())) {
			throw new IllegalStateException("close failed: state is " + stateRef.get());
		}
	}
	
	@Override
	public final boolean isClosed() {
		return stateRef.get().equals(CLOSED);
	}

	public static void closeAll() {
		synchronized(SVC_COROUTINES) {
			for(final Daemon d : SVC_COROUTINES.keySet()) {
				try {
					d.close();
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		}
	}
}
