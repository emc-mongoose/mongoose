package com.emc.mongoose.base.load.step;

import com.emc.mongoose.base.concurrent.Daemon;
import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.commons.concurrent.AsyncRunnable;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface LoadStep extends Daemon {

	/** @return the step id */
	String id() throws RemoteException;

	String getTypeName() throws RemoteException;

	List<? extends AllMetricsSnapshot> metricsSnapshots() throws RemoteException;

	@Override
	AsyncRunnable start() throws RemoteException;

	@Override
	AsyncRunnable await() throws InterruptedException, RemoteException;

	@Override
	boolean await(final long timeout, final TimeUnit timeUnit)
					throws InterruptedException, RemoteException;

	@Override
	AsyncRunnable stop() throws RemoteException;

	void close() throws IOException;
}
