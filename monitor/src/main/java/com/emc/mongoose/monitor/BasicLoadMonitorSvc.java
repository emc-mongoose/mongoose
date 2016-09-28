package com.emc.mongoose.monitor;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.LoadGenerator;
import com.emc.mongoose.model.api.load.LoadMonitorSvc;
import com.emc.mongoose.model.api.load.StorageDriver;
import com.emc.mongoose.model.api.load.StorageDriverSvc;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.util.List;

/**
 Created on 28.09.16.
 */
public class BasicLoadMonitorSvc<I extends Item, O extends IoTask<I>>
extends BasicLoadMonitor<I, O>
implements LoadMonitorSvc<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	public BasicLoadMonitorSvc(
		final String name, final List<LoadGenerator<I, O>> loadGenerators,
		final List<StorageDriver<I, O>> storageDrivers, final Config.LoadConfig loadConfig
	) {
		super(name, loadGenerators, storageDrivers, loadConfig);
		try {
			final String hostName = NetUtil.getHostAddrString();
			for (final StorageDriver<I, O> nextDriver: storageDrivers) {
				if (nextDriver instanceof StorageDriverSvc) {
					final StorageDriverSvc<I, O> nextDriverSvc =
						(StorageDriverSvc<I, O>) nextDriver;
					nextDriverSvc.registerRemotely(hostName, getName());
				}
			}
		} catch(final OmgDoesNotPerformException | OmgLookAtMyConsoleException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to define host name");
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to register monitor service");
		}
	}
}
