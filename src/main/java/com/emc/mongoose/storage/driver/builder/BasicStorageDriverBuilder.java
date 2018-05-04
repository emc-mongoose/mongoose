package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.env.Extensions;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.model.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.model.Constants.KEY_TEST_STEP_ID;

import com.emc.mongoose.storage.driver.StorageDriverFactory;
import com.emc.mongoose.config.item.ItemConfig;
import com.emc.mongoose.config.load.LoadConfig;
import com.emc.mongoose.config.storage.StorageConfig;
import com.emc.mongoose.config.storage.driver.DriverConfig;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> testStepId(final String jobName) {
		this.stepName = jobName;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> dataInput(final DataInput contentSrc) {
		this.contentSrc = contentSrc;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> itemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> loadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
		return this;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> storageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public T build()
	throws OmgShootMyFootException, InterruptedException {

		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepName)
				.put(KEY_CLASS_NAME, BasicStorageDriverBuilder.class.getSimpleName())
		) {

			if(storageConfig == null) {
				throw new OmgShootMyFootException("No storage config is set");
			}
			final DriverConfig driverConfig = storageConfig.getDriverConfig();
			final String driverType = driverConfig.getType();
			if(itemConfig == null) {
				throw new OmgShootMyFootException("No item config config is set");
			}
			final boolean verifyFlag = itemConfig.getDataConfig().getVerify();

			final ServiceLoader<StorageDriverFactory<I, O, T>> loader = ServiceLoader.load(
				(Class) StorageDriverFactory.class, Extensions.CLS_LOADER
			);

			final List<String> availTypes = new ArrayList<>();
			for(final StorageDriverFactory<I, O, T> storageDriverFactory : loader) {
				final String typeName = storageDriverFactory.getName();
				availTypes.add(typeName);
				if(driverType.equals(typeName)) {
					return storageDriverFactory.create(
						stepName, contentSrc, loadConfig, storageConfig, verifyFlag
					);
				}
			}

			throw new OmgShootMyFootException(
				"Failed to create the storage driver for the type \"" + driverType +
					"\", available types: " + Arrays.toString(availTypes.toArray())
			);
		}
	}
}
