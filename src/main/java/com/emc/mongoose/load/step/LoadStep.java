package com.emc.mongoose.load.step;

import com.emc.mongoose.metrics.MetricsSnapshot;

import com.github.akurilov.commons.concurrent.AsyncRunnable;

import java.rmi.RemoteException;
import java.util.List;

public interface LoadStep
extends AsyncRunnable {

	/**
	 @return the step id
	 */
	String id()
	throws RemoteException;

	String getTypeName()
	throws RemoteException;

	List<MetricsSnapshot> metricsSnapshots()
	throws RemoteException;
}
