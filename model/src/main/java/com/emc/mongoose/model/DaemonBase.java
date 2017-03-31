package com.emc.mongoose.model;

import com.emc.mongoose.common.concurrent.Daemon;

import static com.emc.mongoose.common.concurrent.Daemon.State.CLOSED;
import static com.emc.mongoose.common.concurrent.Daemon.State.INITIAL;
import static com.emc.mongoose.common.concurrent.Daemon.State.INTERRUPTED;
import static com.emc.mongoose.common.concurrent.Daemon.State.SHUTDOWN;
import static com.emc.mongoose.common.concurrent.Daemon.State.STARTED;
import static com.emc.mongoose.common.concurrent.ThreadUtil.getHardwareConcurrencyLevel;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created on 12.07.16.
 */
public abstract class DaemonBase
implements Daemon {

	private static final Map<DaemonBase, List<Runnable>> SVC_TASKS = new ConcurrentHashMap<>();
	
	private static final ExecutorService SVC_TASKS_EXECUTOR = Executors.newFixedThreadPool(
		getHardwareConcurrencyLevel(), new NamingThreadFactory("svcTasksWorker", true)
	);
	
	static {
		for(int i = 0; i < getHardwareConcurrencyLevel(); i ++) {
			SVC_TASKS_EXECUTOR.submit(
				() -> {
					DaemonBase nextDaemon;
					List<Runnable> nextDaemonSvcTasks;
					while(true) {
						if(SVC_TASKS.size() == 0) {
							Thread.sleep(1);
						}
						for(final Entry<DaemonBase, List<Runnable>> entry : SVC_TASKS.entrySet()) {
							nextDaemonSvcTasks = entry.getValue();
							nextDaemon = entry.getKey();
							if(nextDaemon.svcTasksLock.tryLock()) {
								try {
									for(final Runnable nextSvcTask : nextDaemonSvcTasks) {
										nextSvcTask.run();
									}
								} catch(final Throwable t) {
									System.err.println(
										"One of the service tasks of of \"" + nextDaemon +
											"\" failed:"
									);
									t.printStackTrace(System.err);
								} finally {
									nextDaemon.svcTasksLock.unlock();
								}
							}
						}
					}
				}
			);
		}
	}
	
	protected final List<Runnable> svcTasks = new ArrayList<>(1);
	protected final Lock svcTasksLock = new ReentrantLock();

	private AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
	protected final Object state = new Object();
	
	protected void doStart()
	throws IllegalStateException {
		SVC_TASKS.put(this, svcTasks);
	}

	protected abstract void doShutdown()
	throws IllegalStateException;

	protected abstract void doInterrupt()
	throws IllegalStateException;
	
	protected void doClose()
	throws IOException, IllegalStateException {
		SVC_TASKS.remove(this);
		svcTasksLock.lock();
		try {
			svcTasks.clear();
		} finally {
			svcTasksLock.unlock();
		}
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
	public final void shutdown()
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
	public final void interrupt()
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
	public void close()
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
}
