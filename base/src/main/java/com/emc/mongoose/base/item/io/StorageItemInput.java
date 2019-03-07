package com.emc.mongoose.base.item.io;

import com.emc.mongoose.base.exception.InterruptRunException;
import com.emc.mongoose.base.item.DataItemFactory;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.storage.driver.StorageDriver;
import com.github.akurilov.commons.io.collection.BufferingInputBase;
import java.io.IOException;
import java.util.List;

/** Created by andrey on 02.12.16. */
public final class StorageItemInput<I extends Item> extends BufferingInputBase<I> {

	private final StorageDriver<I, ? extends Operation<I>> storageDriver;
	private final ItemFactory<I> itemFactory;
	private final String path;
	private final String prefix;
	private final int idRadix;

	private boolean poisonedFlag = false;

	public StorageItemInput(
					final StorageDriver<I, ? extends Operation<I>> storageDriver,
					final int batchSize,
					final ItemFactory<I> itemFactory,
					final String path,
					final String prefix,
					final int idRadix) {
		super(batchSize);
		this.storageDriver = storageDriver;
		this.itemFactory = itemFactory;
		this.path = path;
		this.prefix = prefix;
		this.idRadix = idRadix;
	}

	@Override
	protected final int loadMoreItems(final I lastItem) throws InterruptRunException, IOException {
		if (poisonedFlag) {
			return 0;
		}
		final List<I> newItems = storageDriver.list(itemFactory, path, prefix, idRadix, lastItem, capacity);
		final int n = newItems.size();
		I nextItem;
		for (int i = 0; i < n; i++) {
			nextItem = newItems.get(i);
			if (null == nextItem) {
				poisonedFlag = true;
				return i;
			} else {
				items.add(nextItem);
			}
		}
		return n;
	}

	@Override
	public final String toString() {
		return (itemFactory instanceof DataItemFactory ? "Data" : "") + "ItemsFromPath(" + path + ")";
	}

	@Override
	public final void reset() throws IOException {
		super.reset();
		poisonedFlag = false;
	}
}
