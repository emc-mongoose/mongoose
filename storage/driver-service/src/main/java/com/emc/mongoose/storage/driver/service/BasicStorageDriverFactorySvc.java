package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.ui.config.Config.StorageConfig.StorageType;
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
public class BasicStorageDriverFactorySvc<
	I extends Item & MutableDataItem, O extends IoTask<I> &MutableDataIoTask<I>
	>
extends DaemonBase
implements StorageDriverFactorySvc<I, O, CommonStorageDriverConfigFactory> {

	private static final Logger LOG = LogManager.getLogger();

	public BasicStorageDriverFactorySvc() {
		try {
			ServiceUtil.create(this);
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to create service");
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
		} catch(final DanShootHisFootException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Internal error");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to close service");
		} catch(final NotBoundException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Try to close unbounded service");
		}
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
	}

	@Override
	public final String getName()
	throws RemoteException {
		return SVC_NAME;
	}

	@Override
	public String create(
		final CommonStorageDriverConfigFactory configFactory
	) throws RemoteException {
		final StorageType storageType = configFactory.getStorageType();
		switch(storageType) {
			case HTTP:
				return new HttpStorageDriverFactory<I, O>(configFactory)
					.create(configFactory.getStorageConfig().getHttpConfig().getApi());
			case FS:
				return new FsStorageDriverFactory<I, O>(configFactory)
					.create();
		}
		return null;
	}

}
