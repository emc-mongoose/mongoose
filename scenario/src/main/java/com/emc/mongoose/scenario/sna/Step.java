package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.github.akurilov.concurrent.AsyncRunnable;

import com.emc.mongoose.ui.config.Config;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.rmi.RemoteException;
import java.util.Map;

public interface Step
extends AsyncRunnable {

	/**
	 Configure the step. The actual behavior depends on the particular step type
	 @param config a dictionary of the configuration values to override the inherited config
	 @return <b>new/copied</b> step instance with the applied config values
	 */
	Step config(final Map<String, Object> config)
	throws RemoteException;

	/**
	 @return the step id
	 */
	String id()
	throws RemoteException;

	String getTypeName()
	throws RemoteException;

	MetricsSnapshot metricsSnapshot(final int ioTypeCode)
	throws RemoteException;

	static Config initConfigSlice(final Config config, final String nodeAddrWithPort) {
		final Config configSlice = new Config(config);
		// disable the distributed mode flag
		configSlice.getTestConfig().getStepConfig().setDistributed(false);
		return configSlice;
	}
}
