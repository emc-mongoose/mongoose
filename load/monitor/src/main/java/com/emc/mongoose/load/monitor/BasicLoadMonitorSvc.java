package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitorSvc;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
/**
 Created by andrey on 05.10.16.
 */
public final class BasicLoadMonitorSvc<
	I extends Item, R extends IoResult, O extends IoTask<I, R>

>
extends BasicLoadMonitor<I, R, O>
implements LoadMonitorSvc<R> {

	private static final Logger LOG = LogManager.getLogger();

	/**
	 Single load job constructor
	 @param name
	 @param loadGenerator
	 @param drivers
	 @param loadConfig
	 */
	public BasicLoadMonitorSvc(
		final String name, final LoadGenerator<I, R, O> loadGenerator,
		final List<StorageDriver<I, R, O>> drivers, final LoadConfig loadConfig
	) {
		super(name, loadGenerator, drivers, loadConfig);
	}

	/**
	 Mixed load job constructor
	 @param name
	 @param drivers
	 @param loadConfigs
	 */
	public BasicLoadMonitorSvc(
		final String name,
		final Map<LoadGenerator<I, R, O>, List<StorageDriver<I, R, O>>> drivers,
		final Map<LoadGenerator<I, R, O>, LoadConfig> loadConfigs
	) {
		super(name, drivers, loadConfigs);
	}

	/**
	 Weighted mixed load job constructor
	 @param name
	 @param drivers
	 @param loadConfigs
	 @param weightMap
	 */
	public BasicLoadMonitorSvc(
		final String name,
		final Map<LoadGenerator<I, R, O>, List<StorageDriver<I, R, O>>> drivers,
		final Map<LoadGenerator<I, R, O>, LoadConfig> loadConfigs,
		final Object2IntMap<LoadGenerator<I, R, O>> weightMap
	) {
		super(name, drivers, loadConfigs, weightMap);
	}

	@Override
	protected void registerDrivers(final List<StorageDriver<I, R, O>> drivers) {
		final String hostName;
		try {
			LOG.info(Markers.MSG, "Service started: " + ServiceUtil.create(this));
			hostName = NetUtil.getHostAddrString();
			for(final StorageDriver<I, R, O> nextDriver : drivers) {
				if(nextDriver instanceof StorageDriverSvc) {
					try {
						((StorageDriverSvc<I, R, O>) nextDriver).setOutputSvc(hostName, getName());
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
