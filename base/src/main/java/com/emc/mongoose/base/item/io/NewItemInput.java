package com.emc.mongoose.base.item.io;

import com.emc.mongoose.base.item.DataItemFactory;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.naming.ItemNameInput;
import com.github.akurilov.commons.io.Input;
import java.io.IOException;
import java.util.List;

/** Created by andrey on 01.12.16. */
public class NewItemInput<I extends Item> implements Input<I> {

	protected final ItemFactory<I> itemFactory;
	protected final ItemNameInput itemNameInput;

	public NewItemInput(final ItemFactory<I> itemFactory, final ItemNameInput itemNameInput) {
		this.itemFactory = itemFactory;
		this.itemNameInput = itemNameInput;
	}

	@Override
	public I get() {
		return itemFactory.getItem(itemNameInput.get());
	}

	@Override
	public int get(final List<I> buffer, final int maxCount) {
		for (var i = 0; i < maxCount; i++) {
			buffer.add(itemFactory.getItem(itemNameInput.get()));
		}
		return maxCount;
	}

	/**
	* Skips the specified count of the new item ids
	*
	* @param itemsCount count of items which should be skipped from the beginning
	* @throws IOException doesn't throw
	*/
	@Override
	public final long skip(final long itemsCount) {
		return itemNameInput.skip(itemsCount);
	}

	@Override
	public final void reset() {
		itemNameInput.reset();
	}

	@Override
	public final void close() throws Exception {
		itemNameInput.close();
	}

	@Override
	public String toString() {
		return "New" + (itemFactory instanceof DataItemFactory ? "Data" : "") + "Items";
	}
}
