package com.emc.mongoose.storage.driver;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.concurrent.AsyncRunnable;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 Created on 11.07.16.
 */
public interface StorageDriver<I extends Item, O extends Operation<I>>
extends AsyncRunnable, Input<O>, Output<O> {
	
	int BUFF_SIZE_MIN = 0x1_000;
	int BUFF_SIZE_MAX = 0x1_000_000;
	
	List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws InterruptRunException, IOException;

	@Override
	boolean put(final O op)
	throws InterruptRunException, EOFException;

	@Override
	int put(final List<O> ops, final int from, final int to)
	throws InterruptRunException, EOFException;

	@Override
	int put(final List<O> ops)
	throws InterruptRunException, EOFException;
	
	@Override
	default void reset() {
		throw new AssertionError("Shouldn't be invoked");
	}

	/**
	 * @return 0 if the concurrency is not limited
	 */
	int concurrencyLimit();

	int activeOpCount();
	
	long scheduledOpCount();
	
	long completedOpCount();

	boolean isIdle();

	void adjustIoBuffers(final long avgTransferSize, final OpType opType);

	@Override
	AsyncRunnable stop()
	throws InterruptRunException;

	@Override
	void close()
	throws InterruptRunException, IOException;

	/**
	 @param extensions the resolved runtime extensions
	 @param storageConfig storage sub-config (also specifies the particular storage driver type)
	 @param dataInput the data input used to produce/reproduce the data
	 @param verifyFlag verify the data on read or not
	 @param stepId scenario step id for logging purposes
	 @param <I> item type
	 @param <O> load operation type
	 @param <T> storage driver type
	 @return the storage driver instance
	 @throws IllegalArgumentException if load config either storage config is null
	 @throws InterruptedException may be thrown by a specific storage driver constructor
	 @throws OmgShootMyFootException if no storage driver implementation was found
	 */
	@SuppressWarnings("unchecked")
	static <I extends Item, O extends Operation<I>, T extends StorageDriver<I, O>> T instance(
		final List<Extension> extensions, final Config storageConfig, final DataInput dataInput,
		final boolean verifyFlag, final int batchSize, final String stepId
	) throws IllegalArgumentException, InterruptedException, OmgShootMyFootException {
		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, StorageDriver.class.getSimpleName())
		) {

			if(storageConfig == null) {
				throw new IllegalArgumentException("Null storage config");
			}

			final List<StorageDriverFactory<I, O, T>> factories = extensions
				.stream()
				.filter(ext -> ext instanceof StorageDriverFactory)
				.map(factory -> (StorageDriverFactory<I, O, T>) factory)
				.collect(Collectors.toList());

			final String driverType = storageConfig.stringVal("driver-type");

			final StorageDriverFactory<I, O, T> selectedFactory = factories
				.stream()
				.filter(factory -> driverType.equals(factory.id()))
				.findFirst()
				.orElseThrow(
					() -> new OmgShootMyFootException(
						"Failed to create the storage driver for the type \"" + driverType + "\", available types: " +
							Arrays.toString(factories.stream().map(StorageDriverFactory::id).toArray())
					)
				);
			Loggers.MSG.info("{}: creating the storage driver instance for the type \"{}\"", stepId, driverType);

			return selectedFactory.create(stepId, dataInput, storageConfig, verifyFlag, batchSize);
		}
	}
}
