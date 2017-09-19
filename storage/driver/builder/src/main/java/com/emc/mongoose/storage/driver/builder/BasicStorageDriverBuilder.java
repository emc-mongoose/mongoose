package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.api.common.env.Extensions;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.driver.DriverConfig;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.util.ServiceLoader;

/**
 Created by andrey on 05.10.16.
 */
public class BasicStorageDriverBuilder<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> implements StorageDriverBuilder<I, O, T> {
	
	private String stepName;
	private DataInput contentSrc;
	private ItemConfig itemConfig;
	private LoadConfig loadConfig;
	private AverageConfig avgMetricsConfig;
	private StorageConfig storageConfig;

	protected final String getStepId() {
		return stepName;
	}
	
	@Override
	public ItemConfig getItemConfig() {
		return itemConfig;
	}
	
	@Override
	public LoadConfig getLoadConfig() {
		return loadConfig;
	}

	@Override
	public AverageConfig getAverageConfig() {
		return avgMetricsConfig;
	}

	@Override
	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> setTestStepName(final String jobName) {
		this.stepName = jobName;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setContentSource(final DataInput contentSrc) {
		this.contentSrc = contentSrc;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setItemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
		return this;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> setAverageConfig(
		final AverageConfig avgMetricsConfig
	) {
		this.avgMetricsConfig = avgMetricsConfig;
		return this;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> setStorageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public T build()
	throws UserShootHisFootException, InterruptedException {

		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepName)
				.put(KEY_CLASS_NAME, BasicStorageDriverBuilder.class.getSimpleName())
		) {

			final DriverConfig driverConfig = storageConfig.getDriverConfig();
			final String driverType = driverConfig.getType();
			final boolean verifyFlag = itemConfig.getDataConfig().getVerify();

			final ServiceLoader<StorageDriverFactory<I, O, T>> loader = ServiceLoader.load(
				(Class) StorageDriverFactory.class, Extensions.CLS_LOADER
			);

			for(final StorageDriverFactory<I, O, T> storageDriverFactory : loader) {
				if(driverType.equals(storageDriverFactory.getName())) {
					return storageDriverFactory.create(
						stepName, contentSrc, loadConfig, storageConfig, verifyFlag
					);
				}
			}

			Loggers.ERR.fatal(
				"Failed to create the storage driver for the type \"{}\"", driverType
			);
			return null;
		}
	}
}
