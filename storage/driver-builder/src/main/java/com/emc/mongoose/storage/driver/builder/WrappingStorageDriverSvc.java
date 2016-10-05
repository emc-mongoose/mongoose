package com.emc.mongoose.storage.driver.builder;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.LoadMonitor;
import com.emc.mongoose.model.api.load.LoadMonitorSvc;
import com.emc.mongoose.model.api.storage.StorageDriver;
import com.emc.mongoose.model.api.storage.StorageDriverSvc;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 05.10.16.
 */
public final class WrappingStorageDriverSvc<I extends Item, O extends IoTask<I>>
implements StorageDriverSvc<I, O> {

	private final StorageDriver<I, O> driver;
	private final String runId;

	public WrappingStorageDriverSvc(final StorageDriver<I, O> driver, final String runId) {
		this.driver = driver;
		this.runId = runId;
	}

	@Override
	public final String getName()
	throws RemoteException {
		return driver.getClass().getSimpleName() + "-" + runId;
	}

	@Override
	public final void start()
	throws IllegalStateException, RemoteException {
		driver.start();
		ServiceUtil.create(this);
	}

	@Override
	public final void registerRemotely(final String addr, final String monitorSvcName)
	throws RemoteException {
		final LoadMonitorSvc<I, O> monitorSvc = ServiceUtil.resolve(addr, monitorSvcName);
		driver.register(monitorSvc);
	}

	@Override
	public final void close()
	throws IOException {
		try {
			driver.close();
		} finally {
			ServiceUtil.close(this);
		}
	}

	// just wrapping methods below

	@Override
	public final boolean isStarted()
	throws RemoteException {
		return driver.isStarted();
	}

	@Override
	public final void shutdown()
	throws IllegalStateException, RemoteException {
		driver.shutdown();
	}

	@Override
	public final boolean isShutdown()
	throws RemoteException {
		return driver.isShutdown();
	}

	@Override
	public final void await()
	throws InterruptedException, RemoteException {
		driver.await();
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		return driver.await(timeout, timeUnit);
	}

	@Override
	public final void interrupt()
	throws IllegalStateException, RemoteException {
		driver.interrupt();
	}

	@Override
	public final boolean isInterrupted()
	throws RemoteException {
		return driver.isInterrupted();
	}

	@Override
	public final boolean isClosed()
	throws RemoteException {
		return driver.isClosed();
	}

	@Override
	public final void put(final O item)
	throws IOException {
		driver.put(item);
	}

	@Override
	public final int put(final List<O> buffer, final int from, final int to)
	throws IOException {
		return driver.put(buffer, from, to);
	}

	@Override
	public final int put(final List<O> buffer)
	throws IOException {
		return driver.put(buffer);
	}

	@Override
	public final Input<O> getInput()
	throws IOException {
		throw new RemoteException();
	}

	@Override
	public final void register(final LoadMonitor<I, O> monitor)
	throws RemoteException {
		throw new RemoteException();
	}

	@Override
	public final boolean isIdle()
	throws RemoteException {
		return driver.isIdle();
	}

	@Override
	public final boolean isFullThrottleEntered()
	throws RemoteException {
		return driver.isFullThrottleEntered();
	}

	@Override
	public final boolean isFullThrottleExited()
	throws RemoteException {
		return driver.isFullThrottleExited();
	}
}