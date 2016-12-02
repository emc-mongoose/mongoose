package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.io.collection.BufferingInputBase;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.StorageDriver;

import java.util.List;
/**
 Created by andrey on 02.12.16.
 */
public final class StorageListingInput<I extends Item>
extends BufferingInputBase<I> {

	private final StorageDriver<I, ? extends IoTask<I, ?>, ? extends IoResult> storageDriver;
	private final ItemFactory<I> itemFactory;

	public StorageListingInput(
		final StorageDriver<I, ? extends IoTask<I, ?>, ? extends IoResult> storageDriver,
		final ItemFactory<I> itemFactory,
		final int capacity
	) {
		super(capacity);
		this.storageDriver = storageDriver;
		this.itemFactory = itemFactory;
	}

	@Override
	protected final int loadMoreItems() {
		final List<I> newItems = storageDriver.list(i)
		return 0;
	}
}
