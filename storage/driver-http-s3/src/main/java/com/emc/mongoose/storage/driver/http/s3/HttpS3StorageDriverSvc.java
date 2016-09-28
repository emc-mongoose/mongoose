package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
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

import java.net.MalformedURLException;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 Created on 28.09.16.
 */
public class HttpS3StorageDriverSvc<I extends Item, O extends IoTask<I>>
extends HttpS3StorageDriver<I, O>
implements StorageDriverSvc<I, O>{

	private static final Logger LOG = LogManager.getLogger();

	public HttpS3StorageDriverSvc(
		final String runId, final Config.LoadConfig loadConfig,
		final Config.StorageConfig storageConfig, final String srcContainer,
		final boolean verifyFlag, final Config.SocketConfig socketConfig
	)
	throws UserShootHisFootException {
		super(runId, loadConfig, storageConfig, srcContainer, verifyFlag, socketConfig);
	}

	@Override
	public void registerRemotely(final String hostName, final String monitorSvcName)
	throws RemoteException {
		try {
			final LoadMonitorSvc<I, O> monitorSvc =
				ServiceUtil.getSvc(hostName, monitorSvcName);
			register(monitorSvc);
		} catch(NotBoundException | MalformedURLException | SocketException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to close service");
		} catch(final OmgLookAtMyConsoleException | OmgDoesNotPerformException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		}
	}

	@Override
	public String getName()
	throws RemoteException {
		return getClass().getCanonicalName();
	}
}
