package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.io.collection.BufferingInputBase;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import com.emc.mongoose.model.item.DataItemFactory;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.StorageDriver;

import java.io.IOException;

/**
 Created by andrey on 02.12.16.
 */
public final class StorageItemInput<I extends Item>
extends BufferingInputBase<I> {

	private final StorageDriver<I, ? extends IoTask<I>> storageDriver;
	private final ItemFactory<I> itemFactory;
	private final String path;
	private final String prefix;
	private final int idRadix;

	public StorageItemInput(
		final StorageDriver<I, ? extends IoTask<I>> storageDriver,
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix
	) {
		super(BATCH_SIZE);
		this.storageDriver = storageDriver;
		this.itemFactory = itemFactory;
		this.path = path;
		this.prefix = prefix;
		this.idRadix = idRadix;
	}

	@Override
	protected final int loadMoreItems(final I lastItem)
	throws IOException {
		items.addAll(storageDriver.list(itemFactory, path, prefix, idRadix, lastItem, BATCH_SIZE));
		return items.size();
	}

	@Override
	public final String toString() {
		return (itemFactory instanceof DataItemFactory ? "Data" : "") +
			"ItemsFromPath(" + path + ")";
	}
}
