package com.emc.mongoose.load.generator;

import com.emc.mongoose.api.common.io.collection.BufferingInputBase;
import com.emc.mongoose.api.model.io.task.IoTask;

import com.emc.mongoose.api.model.item.DataItemFactory;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.storage.StorageDriver;

import java.io.IOException;
import java.util.List;

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
		final StorageDriver<I, ? extends IoTask<I>> storageDriver, final int batchSize,
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix
	) {
		super(batchSize);
		this.storageDriver = storageDriver;
		this.itemFactory = itemFactory;
		this.path = path;
		this.prefix = prefix;
		this.idRadix = idRadix;
	}

	@Override
	protected final int loadMoreItems(final I lastItem)
	throws IOException {
		final List<I>
			newItems = storageDriver.list(itemFactory, path, prefix, idRadix, lastItem, capacity);
		for(final I item : newItems) {
			items.add(item);
		}
		return items.size();
	}

	@Override
	public final String toString() {
		return (itemFactory instanceof DataItemFactory ? "Data" : "") +
			"ItemsFromPath(" + path + ")";
	}
}
