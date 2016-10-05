package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.storage.StorageDriver;
import com.emc.mongoose.model.api.storage.StorageDriverSvc;
import com.emc.mongoose.ui.config.Config.IoConfig;
import com.emc.mongoose.ui.config.Config.ItemConfig;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 05.10.16.
 */
public class BasicStorageDriverBuilderSvc<
	I extends Item, O extends IoTask<I>, T extends StorageDriverSvc<I, O>
> extends BasicStorageDriverBuilder<I, O, T> implements StorageDriverBuilderSvc<I, O, T> {

	private static final Logger LOG = LogManager.getLogger();

	@Override
	public StorageDriverBuilderSvc<I, O, T> setRunId(final String runId) {
		super.setRunId(runId);
		return this;
	}

	@Override
	public StorageDriverBuilderSvc<I, O, T> setItemConfig(final ItemConfig itemConfig) {
		super.setItemConfig(itemConfig);
		return this;
	}

	@Override
	public StorageDriverBuilderSvc<I, O, T> setLoadConfig(final LoadConfig loadConfig) {
		super.setLoadConfig(loadConfig);
		return this;
	}

	@Override
	public StorageDriverBuilderSvc<I, O, T> setIoConfig(final IoConfig ioConfig) {
		super.setIoConfig(ioConfig);
		return this;
	}

	@Override
	public StorageDriverBuilderSvc<I, O, T> setStorageConfig(final StorageConfig storageConfig) {
		super.setStorageConfig(storageConfig);
		return this;
	}

	@Override
	public StorageDriverBuilderSvc<I, O, T> setSocketConfig(final SocketConfig socketConfig) {
		super.setSocketConfig(socketConfig);
		return this;
	}

	@Override
	public void start()
	throws IllegalStateException, RemoteException {
		LOG.info(Markers.MSG, "Storage driver builder service started: " + ServiceUtil.create(this));
	}

	@Override
	public boolean isStarted()
	throws RemoteException {
		return false;
	}

	@Override
	public void shutdown()
	throws IllegalStateException, RemoteException {
	}

	@Override
	public boolean isShutdown()
	throws RemoteException {
		return false;
	}

	@Override
	public final void await()
	throws InterruptedException, RemoteException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		timeUnit.sleep(timeout);
		return true;
	}

	@Override
	public final void interrupt()
	throws RemoteException {
		try {
			close();
		} catch(final IOException ignore) {
		}
	}

	@Override
	public final boolean isInterrupted()
	throws RemoteException {
		return false;
	}

	@Override
	public final boolean isClosed()
	throws RemoteException {
		return false;
	}

	@Override
	public final String getName()
	throws RemoteException {
		return SVC_NAME;
	}

	@Override
	public final void close()
	throws IOException {
		ServiceUtil.close(this);
		LOG.info(Markers.MSG, "Service \"{}\" closed", getName());
	}

	@Override @SuppressWarnings("unchecked")
	public final String buildRemotely()
	throws RemoteException, UserShootHisFootException {
		final StorageDriver<I, O> driver = build();
		final T wrapper = (T) new WrappingStorageDriverSvc<>(driver, getRunId());
		return wrapper.getName();
	}
}
