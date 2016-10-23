package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.ItemFactory;
import com.emc.mongoose.common.api.SizeInBytes;
import static com.emc.mongoose.model.api.io.task.IoTask.SLASH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class NewDataItemInput<D extends DataItem>
implements Input<D> {
	//
	private final ItemFactory<D> itemFactory;
	private final BasicItemNameInput idInput;
	private final SizeInBytes dataSize;
	//
	public NewDataItemInput(
		final ItemFactory<D> itemFactory, final BasicItemNameInput idInput,
		final SizeInBytes dataSize
	) throws IllegalArgumentException {
		this.itemFactory = itemFactory;
		this.idInput = idInput;
		this.dataSize = dataSize;
	}
	//
	public SizeInBytes getDataSizeInfo() {
		return dataSize;
	}
	//
	@Override
	public final D get()
	throws IOException {
		return itemFactory.getItem(idInput.get(), idInput.getLastValue(), dataSize.get());
	}
	//
	@Override
	public int get(final List<D> buffer, final int maxCount)
	throws IOException {
		for(int i = 0; i < maxCount; i ++) {
			buffer.add(itemFactory.getItem(idInput.get(), idInput.getLastValue(), dataSize.get()));
		}
		return maxCount;
	}
	/**
	 * Does nothing
	 * @param itemsCount count of items which should be skipped from the beginning
	 * @throws IOException doesn't throw
	 */
	@Override
	public void skip(final long itemsCount)
	throws IOException {
	}
	//
	@Override
	public final void reset() {
	}
	//
	@Override
	public final void close()
	throws IOException {
	}
	//
	@Override
	public final String toString() {
		return "newDataItemSrc<" + itemFactory.getClass().getSimpleName() + ">";
	}
}
