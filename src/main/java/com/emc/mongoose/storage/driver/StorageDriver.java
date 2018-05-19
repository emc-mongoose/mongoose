package com.emc.mongoose.storage.driver;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.concurrent.AsyncRunnable;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

/**
 Created on 11.07.16.
 */
public interface StorageDriver<I extends Item, O extends IoTask<I>>
extends AsyncRunnable, Input<O>, Output<O> {
	
	int BUFF_SIZE_MIN = 0x1_000;
	int BUFF_SIZE_MAX = 0x1_000_000;
	
	List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int
		idRadix,
		final I lastPrevItem, final int count
	) throws IOException;

	@Override
	default int get(final List<O> buff, final int limit) {
		throw new AssertionError("Shouldn't be invoked");
	}
	
	@Override
	default void reset() {
		throw new AssertionError("Shouldn't be invoked");
	}

	/**
	 * @return 0 if the concurrency is not limited
	 */
	int getConcurrencyLevel();

	int getActiveTaskCount();
	
	long getScheduledTaskCount();
	
	long getCompletedTaskCount();

	boolean isIdle();

	void adjustIoBuffers(final long avgTransferSize, final IoType ioType);

	/**

	 @param clsLoader the classloader to use to find the storage driver implementation
	 @param loadConfig load sub-config
	 @param storageConfig storage sub-config (also specifies the particular storage driver type)
	 @param dataInput the data input used to produce/reproduce the data
	 @param verifyFlag verify the data on read or not
	 @param stepId scenario step id for logging purposes
	 @param <I> item type
	 @param <O> I/O task type
	 @param <T> storage driver type
	 @return the storage driver instance
	 @throws IllegalArgumentException if load config either storage config is null
	 @throws InterruptedException may be thrown by a specific storage driver constructor
	 @throws OmgShootMyFootException if no storage driver implementation was found
	 */
	@SuppressWarnings("unchecked")
	static <I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>> T instance(
		final ClassLoader clsLoader, final Config loadConfig, final Config storageConfig,
		final DataInput dataInput, final boolean verifyFlag, final String stepId
	) throws IllegalArgumentException, InterruptedException, OmgShootMyFootException {
		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, StorageDriver.class.getSimpleName())
		) {
			if(loadConfig == null) {
				throw new IllegalArgumentException("Null load config");
			}
			if(storageConfig == null) {
				throw new IllegalArgumentException("Null storage config");
			}
			final ServiceLoader<StorageDriverFactory<I, O, T>> loader = ServiceLoader.load(
				(Class) StorageDriverFactory.class, clsLoader
			);
			final Collection<String> availTypes = new ArrayList<>();
			final String driverType = storageConfig.stringVal("driver-type");
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
