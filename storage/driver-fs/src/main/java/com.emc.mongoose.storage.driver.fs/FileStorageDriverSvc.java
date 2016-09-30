package com.emc.mongoose.storage.driver.fs;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.LoadMonitorSvc;
import com.emc.mongoose.model.api.load.StorageDriverSvc;
import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 Created on 30.09.16.
 */
public class FileStorageDriverSvc<I extends MutableDataItem, O extends MutableDataIoTask<I>>
extends FileStorageDriver<I, O>
implements StorageDriverSvc<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	public FileStorageDriverSvc(
		final String runId, final Config.LoadConfig loadConfig,
		final String srcContainer, final StorageConfig storageConfig,
		final boolean verifyFlag, final SizeInBytes ioBuffSize
	) {
		super(runId, loadConfig, srcContainer, storageConfig, verifyFlag, ioBuffSize);
		try {
			ServiceUtil.create(this);
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to create service");
		}
	}

	// TODO eliminate duplication
	@Override
	public void registerRemotely(final String hostName, final String monitorSvcName)
	throws RemoteException {
		final LoadMonitorSvc<I, O> monitorSvc;
		try {
			monitorSvc = ServiceUtil.getSvc(hostName, monitorSvcName);
			register(monitorSvc);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to get service");
		} catch(final NotBoundException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Try to close unbounded service");
		}
	}

	@Override
	public String getName()
	throws RemoteException {
		return getClass().getSimpleName().toLowerCase() + "/" + runId;
	}

	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		super.doInterrupt();
		try {
			ServiceUtil.close(this);
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to close service");
		} catch(final NotBoundException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Try to close unbounded service");
		}
	}
}
