package com.emc.mongoose.base.item.io;

import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.naming.ItemNameInput;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.system.SizeInBytes;
import java.util.List;

public final class NewDataItemInput<D extends DataItem> extends NewItemInput<D>
				implements Input<D> {

	private final SizeInBytes dataSize;

	public NewDataItemInput(
					final ItemFactory<D> itemFactory, final ItemNameInput itemNameInput, final SizeInBytes dataSize) {
		super(itemFactory, itemNameInput);
		this.dataSize = dataSize;
	}

	@Override
	public final D get() {
		return itemFactory.getItem(itemNameInput.get(), itemNameInput.lastId(), dataSize.get());
	}

	@Override
	public final int get(final List<D> buffer, final int maxCount) {
		for (var i = 0; i < maxCount; i++) {
			buffer.add(itemFactory.getItem(itemNameInput.get(), itemNameInput.lastId(), dataSize.get()));
		}
		return maxCount;
	}

	@Override
	public final String toString() {
		return super.toString() + "(" + dataSize.toString() + ")";
	}
}
