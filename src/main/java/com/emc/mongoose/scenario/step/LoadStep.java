package com.emc.mongoose.scenario.step;

import com.emc.mongoose.model.metrics.MetricsSnapshot;
import com.emc.mongoose.config.Config;

import com.github.akurilov.concurrent.AsyncRunnable;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface LoadStep
extends AsyncRunnable {

	/**
	 Configure the step. The actual behavior depends on the particular step type
	 @param config a dictionary of the configuration values to override the inherited config
	 @return <b>new/copied</b> step instance with the applied config values
	 */
	LoadStep config(final Map<String, Object> config)
	throws RemoteException;

	/**
	 @return the step id
	 */
	String id()
	throws RemoteException;

	String getTypeName()
	throws RemoteException;

	List<MetricsSnapshot> metricsSnapshots()
	throws RemoteException;

	static Config initConfigSlice(final Config config, final String nodeAddrWithPort) {
		final Config configSlice = new Config(config);
		// disable the distributed mode flag
		configSlice.getScenarioConfig().getStepConfig().setDistributed(false);
		return configSlice;
	}
}
