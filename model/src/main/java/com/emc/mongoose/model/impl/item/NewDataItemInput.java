package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.ItemFactory;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.model.util.SizeInBytes;

import java.io.IOException;
import java.util.List;

public final class NewDataItemInput<D extends DataItem>
implements Input<D> {
	//
	private final ItemFactory<D> itemFactory;
	private final Input<String> pathInput;
	private final BasicItemNameInput idInput;
	private final ContentSource contentSrc;
	private final SizeInBytes dataSize;
	//
	public NewDataItemInput(
		final ItemFactory<D> itemFactory, final Input<String> pathInput,
		final BasicItemNameInput idInput, final ContentSource contentSrc, final SizeInBytes dataSize
	) throws IllegalArgumentException {
		this.itemFactory = itemFactory;
		this.pathInput = pathInput;
		this.idInput = idInput;
		this.contentSrc = ContentSourceUtil.clone(contentSrc);
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
		return itemFactory.getInstance(
			pathInput.get(), idInput.get(), idInput.getLastValue(), dataSize.get(), contentSrc
		);
	}
	//
	@Override
	public int get(final List<D> buffer, final int maxCount)
	throws IOException {
		for(int i = 0; i < maxCount; i ++) {
			buffer.add(
				itemFactory.getInstance(
					pathInput.get(), idInput.get(), idInput.getLastValue(), dataSize.get(),
					contentSrc
				)
			);
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
		if(contentSrc != null) {
			contentSrc.close();
		}
	}
	//
	@Override
	public final String toString() {
		return "newDataItemSrc<" + itemFactory.getClass().getDeclaringClass().getSimpleName() + ">";
	}
}
