package com.emc.mongoose.monitor;

import com.emc.mongoose.common.exception.DanShootHisFootException;
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
		final String name, final List<LoadGenerator<I, O>> generators,
		final List<StorageDriver<I, O>> drivers, final Config.LoadConfig loadConfig
	) {
		super(name, generators, drivers, loadConfig);
	}

	@Override
	protected void registerDrivers(final List<StorageDriver<I, O>> drivers) {
		final String hostName;
		try {
			hostName = NetUtil.getHostAddrString();
			for (final StorageDriver<I, O> nextDriver: drivers) {
				if (nextDriver instanceof StorageDriverSvc) {
					final StorageDriverSvc<I, O>
						nextDriverSvc = (StorageDriverSvc<I, O>) nextDriver;
					nextDriverSvc.registerRemotely(hostName, getName());
				}
			}
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to determine host name");
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to register monitor service");
		}
	}
}
