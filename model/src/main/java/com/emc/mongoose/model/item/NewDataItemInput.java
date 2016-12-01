package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.api.SizeInBytes;

import java.io.IOException;
import java.util.List;

public final class NewDataItemInput<D extends DataItem>
extends NewItemInput<D>
implements Input<D> {
	
	private final SizeInBytes dataSize;
	
	public NewDataItemInput(
		final ItemFactory<D> itemFactory, final IdStringInput idInput,
		final SizeInBytes dataSize
	) {
		super(itemFactory, idInput);
		this.dataSize = dataSize;
	}
	
	public SizeInBytes getDataSizeInfo() {
		return dataSize;
	}
	
	@Override
	public final D get()
	throws IOException {
		return itemFactory.getItem(idInput.get(), idInput.getLastValue(), dataSize.get());
	}
	
	@Override
	public final int get(final List<D> buffer, final int maxCount)
	throws IOException {
		for(int i = 0; i < maxCount; i ++) {
			buffer.add(itemFactory.getItem(idInput.get(), idInput.getLastValue(), dataSize.get()));
		}
		return maxCount;
	}
	
	@Override
	public final String toString() {
		return "newDataItemInput<" + itemFactory.getClass().getSimpleName() + ">";
	}
}
