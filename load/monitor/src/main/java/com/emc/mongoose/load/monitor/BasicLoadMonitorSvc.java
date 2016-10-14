package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.LoadGenerator;
import com.emc.mongoose.model.api.load.LoadMonitorSvc;
import com.emc.mongoose.model.api.storage.StorageDriver;
import com.emc.mongoose.model.api.storage.StorageDriverSvc;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

/**
 Created by andrey on 05.10.16.
 */
public final class BasicLoadMonitorSvc<I extends Item, O extends IoTask<I>>
extends BasicLoadMonitor<I, O>
implements LoadMonitorSvc<I,O> {

	private final static Logger LOG = LogManager.getLogger();

	public BasicLoadMonitorSvc(
		final String name, final List<LoadGenerator<I, O>> loadGenerators,
		final List<StorageDriver<I, O>> storageDrivers, final Config.LoadConfig loadConfig
	) {
		super(name, loadGenerators, storageDrivers, loadConfig);
	}

	@Override
	protected void registerDrivers(final List<StorageDriver<I, O>> drivers) {
		final String hostName;
		try {
			LOG.info(Markers.MSG, "Service started: " + ServiceUtil.create(this));
			hostName = NetUtil.getHostAddrString();
			for(final StorageDriver<I, O> nextDriver : drivers) {
				if(nextDriver instanceof StorageDriverSvc) {
					try {
						((StorageDriverSvc<I, O>) nextDriver).registerRemotely(hostName, getName());
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.DEBUG, e,
							"Failed to register the load monitor service remotely"
						);
					}
				}
			}
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to determine host name");
		}
	}

	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		super.doInterrupt();
		try {
			LOG.info(Markers.MSG, "Service closed: " + ServiceUtil.close(this));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to close service");
		}
	}
}
