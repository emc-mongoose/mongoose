package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.StorageType;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 Created on 28.09.16.
 */
public class BasicStorageDriverFactorySvc
extends DaemonBase
implements StorageDriverFactorySvc {

	private static final Logger LOG = LogManager.getLogger();

	public BasicStorageDriverFactorySvc() {
		try {
			ServiceUtil.create(this);
		} catch(final RemoteException | MalformedURLException | SocketException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to create service");
		} catch(final OmgLookAtMyConsoleException | OmgDoesNotPerformException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		}
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
	}

	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		try {
			ServiceUtil.close(this);
		} catch(
			final RemoteException | MalformedURLException |
				SocketException | NotBoundException e
			) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to close service");
		} catch(final OmgLookAtMyConsoleException | OmgDoesNotPerformException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		}
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
	}

	@Override
	public String getName()
	throws RemoteException {
		return SVC_NAME;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		timeUnit.sleep(timeout);
		return true;
	}

	@Override
	public String create(final CommonStorageDriverConfigFactory configFactory)
	throws RemoteException {
		final StorageType storageType = configFactory.getStorageType();
		switch(storageType) {
			case HTTP:
				return new HttpStorageDriverFactory(configFactory)
//					.create(configFactory.getStorageConfig().getHttpConfig().getApi());
					.create(HttpStorageDriverFactory.Api.S3);
			case FS:
				throw new UnsupportedOperationException();
		}
		return null;
	}
}
