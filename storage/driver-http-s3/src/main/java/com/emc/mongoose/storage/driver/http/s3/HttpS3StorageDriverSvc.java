package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.LoadMonitorSvc;
import com.emc.mongoose.model.api.load.StorageDriverSvc;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 Created on 28.09.16.
 */
public class HttpS3StorageDriverSvc<I extends Item, O extends IoTask<I>>
extends HttpS3StorageDriver<I, O>
implements StorageDriverSvc<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	public HttpS3StorageDriverSvc(
		final String runId, final Config.LoadConfig loadConfig,
		final String srcContainer, final Config.StorageConfig storageConfig,
		final boolean verifyFlag, final Config.SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(runId, loadConfig, srcContainer, storageConfig, verifyFlag, socketConfig);
		try {
			ServiceUtil.create(this);
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to create service");
		}
	}

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
	public final String getName()
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
