package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.api.common.concurrent.SvcTask;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.common.net.ServiceUtil;
import com.emc.mongoose.api.model.data.ContentSource;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.api.model.storage.StorageDriverSvc;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.Loggers;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 05.10.16.
 */
public class BasicStorageDriverBuilderSvc<
	I extends Item, O extends IoTask<I>, T extends StorageDriverSvc<I, O>
>
extends BasicStorageDriverBuilder<I, O, T>
implements StorageDriverBuilderSvc<I, O, T> {

	private final int port;

	public BasicStorageDriverBuilderSvc(final int port) {
		this.port = port;
	}

	@Override
	public BasicStorageDriverBuilderSvc<I, O, T> setTestStepName(final String jobName) {
		super.setTestStepName(jobName);
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilderSvc<I, O, T> setContentSource(final ContentSource contentSrc) {
		super.setContentSource(contentSrc);
		return this;
	}

	@Override
	public BasicStorageDriverBuilderSvc<I, O, T> setItemConfig(final ItemConfig itemConfig) {
		super.setItemConfig(itemConfig);
		return this;
	}

	@Override
	public BasicStorageDriverBuilderSvc<I, O, T> setLoadConfig(final LoadConfig loadConfig) {
		super.setLoadConfig(loadConfig);
		return this;
	}

	@Override
	public BasicStorageDriverBuilderSvc<I, O, T> setAverageConfig(
		final AverageConfig avgMetricsConfig
	) {
		super.setAverageConfig(avgMetricsConfig);
		return this;
	}


	@Override
	public BasicStorageDriverBuilderSvc<I, O, T> setStorageConfig(final StorageConfig storageConfig) {
		super.setStorageConfig(storageConfig);
		return this;
	}

	@Override
	public final List<SvcTask> getSvcTasks() {
		throw new AssertionError("Shouldn't be invoked");
	}
	
	@Override
	public final State getState()
	throws RemoteException {
		return State.INITIAL;
	}
	
	@Override
	public void start()
	throws IllegalStateException, RemoteException {
		Loggers.MSG.info("Service started: " + ServiceUtil.create(this, port));
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
	public final int getRegistryPort()
	throws RemoteException {
		return port;
	}

	@Override
	public final String getName()
	throws RemoteException {
		return SVC_NAME;
	}

	@Override
	public final void close()
	throws IOException {
		Loggers.MSG.info("Service closed: " + ServiceUtil.close(this));
	}

	@Override @SuppressWarnings("unchecked")
	public final String buildRemotely()
	throws IOException, UserShootHisFootException {
		final StorageDriver<I, O> driver = build();
		final T wrapper = (T) new WrappingStorageDriverSvc<>(
			port, driver, getAverageConfig().getPeriod(), getStepName()
		);
		return wrapper.getName();
	}
}
