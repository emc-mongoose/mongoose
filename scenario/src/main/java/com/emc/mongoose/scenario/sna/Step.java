package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.concurrent.AsyncRunnable;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
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

	static String initConfigSlices(
		final Config config, final Map<String, Config> configSlices, final String nodeAddrWithPort
	) {
		final Config configSlice = new Config(config);
		// disable the distributed mode flag
		configSlice.getTestConfig().getStepConfig().setDistributed(false);
		configSlices.put(nodeAddrWithPort, configSlice);
		return nodeAddrWithPort;
	}

	static String setConfigSlicesItemInputFile(
		final Map<String, Config> configSlices, final Map<String, FileService> fileSvcs,
		final String nodeAddrWithPort
	) {
		final FileService fileSvc = fileSvcs.get(nodeAddrWithPort);
		final InputConfig inputConfigSlice = configSlices
			.get(nodeAddrWithPort)
			.getItemConfig()
			.getInputConfig();
		try {
			inputConfigSlice.setFile(fileSvc.getFilePath());
		} catch(final RemoteException e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to invoke the remote method @{}",
				nodeAddrWithPort
			);
		}
		return nodeAddrWithPort;
	}
}
