package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import com.emc.mongoose.storage.driver.StorageDriverFactory;

import com.github.akurilov.confuse.Config;

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

	private ClassLoader clsLoader;
	private String stepId;
	private DataInput dataInput;
	private Config itemConfig;
	private Config loadConfig;
	private Config storageConfig;

	protected final String stepId() {
		return stepId;
	}
	
	@Override
	public Config itemConfig() {
		return itemConfig;
	}
	
	@Override
	public Config loadConfig() {
		return loadConfig;
	}

	@Override
	public Config storageConfig() {
		return storageConfig;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> classLoader(final ClassLoader clsLoader) {
		this.clsLoader = clsLoader;
		return this;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> testStepId(final String jobName) {
		this.stepId = jobName;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> dataInput(final DataInput contentSrc) {
		this.dataInput = contentSrc;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> itemConfig(final Config itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> loadConfig(final Config loadConfig) {
		this.loadConfig = loadConfig;
		return this;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> storageConfig(final Config storageConfig) {
		this.storageConfig = storageConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public T build()
	throws OmgShootMyFootException, InterruptedException {

		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, BasicStorageDriverBuilder.class.getSimpleName())
		) {

			if(storageConfig == null) {
				throw new OmgShootMyFootException("No storage config is set");
			}
			final Config driverConfig = storageConfig.configVal("driver");
			final String driverType = driverConfig.stringVal("type");
			if(itemConfig == null) {
				throw new OmgShootMyFootException("No item config config is set");
			}
			final boolean verifyFlag = itemConfig.boolVal("data-verify");

			final ServiceLoader<StorageDriverFactory<I, O, T>> loader = ServiceLoader.load(
				(Class) StorageDriverFactory.class, clsLoader
			);

			final List<String> availTypes = new ArrayList<>();
			for(final StorageDriverFactory<I, O, T> storageDriverFactory : loader) {
				final String typeName = storageDriverFactory.getName();
				availTypes.add(typeName);
				if(driverType.equals(typeName)) {
					return storageDriverFactory.create(
						stepId, dataInput, loadConfig, storageConfig, verifyFlag
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
