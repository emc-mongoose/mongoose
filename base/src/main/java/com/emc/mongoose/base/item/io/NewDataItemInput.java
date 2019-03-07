package com.emc.mongoose.base.item.io;

import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.IdStringInput;
import com.emc.mongoose.base.item.ItemFactory;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.system.SizeInBytes;
import java.io.IOException;
import java.util.List;

public final class NewDataItemInput<D extends DataItem> extends NewItemInput<D>
				implements Input<D> {

	private final SizeInBytes dataSize;

	public NewDataItemInput(
					final ItemFactory<D> itemFactory, final IdStringInput idInput, final SizeInBytes dataSize) {
		super(itemFactory, idInput);
		this.dataSize = dataSize;
	}

	@Override
	public final D get() throws IOException {
		return itemFactory.getItem(idInput.get(), idInput.getAsLong(), dataSize.get());
	}

	@Override
	public final int get(final List<D> buffer, final int maxCount) throws IOException {
		for (int i = 0; i < maxCount; i++) {
			buffer.add(itemFactory.getItem(idInput.get(), idInput.getAsLong(), dataSize.get()));
		}
		return maxCount;
	}

	@Override
	public final String toString() {
		return super.toString() + "(" + dataSize.toString() + ")";
	}
}
