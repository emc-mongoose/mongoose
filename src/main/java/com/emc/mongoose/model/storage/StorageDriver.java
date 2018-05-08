package com.emc.mongoose.model.storage;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.concurrent.AsyncRunnable;

import java.io.IOException;
import java.util.List;

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

	@Override
	void close()
	throws IOException;
}
