package com.emc.mongoose.core.impl.item.data;
//
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.base.ItemSrc;
//
import com.emc.mongoose.core.impl.item.base.BasicItemNameGenerator;
//
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
/**
 Created by kurila on 24.07.15.
 */
public final class NewDataItemSrc<T extends DataItem>
implements ItemSrc<T> {
	//
	private final Constructor<T> itemConstructor;
	private final BasicItemNameGenerator idGenerator;
	private final ContentSource contentSrc;
	private final SizeInBytes dataSize;
	private T lastItem = null;
	//
	public NewDataItemSrc(
		final Class<T> dataCls, final BasicItemNameGenerator idGenerator,
		final ContentSource contentSrc, final SizeInBytes dataSize
	) throws NoSuchMethodException, IllegalArgumentException {
		this.itemConstructor = dataCls.getConstructor(
			String.class, Long.class, Long.class, ContentSource.class
		);
		this.idGenerator = idGenerator;
		this.contentSrc = contentSrc;
		this.dataSize = dataSize;
	}
	//
	public SizeInBytes getDataSizeInfo() {
		return dataSize;
	}
	//
	@Override
	public final T get()
	throws IOException {
		try {
			return itemConstructor.newInstance(
				idGenerator.get(), idGenerator.getLastValue(), dataSize.get(), contentSrc
			);
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public int get(final List<T> buffer, final int maxCount)
	throws IOException {
		try {
			for(int i = 0; i < maxCount; i ++) {
				buffer.add(
					itemConstructor.newInstance(
						idGenerator.get(), idGenerator.getLastValue(), dataSize.get(), contentSrc
					)
				);
			}
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
		}
		return maxCount;
	}
	//
	@Override
	public T getLastItem() {
		return lastItem;
	}
	//
	@Override
	public void setLastItem(final T lastItem) {
		this.lastItem = lastItem;
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
	public final void close() {
	}
	//
	@Override
	public final String toString() {
		return "newDataItemSrc<" + itemConstructor.getDeclaringClass().getSimpleName() + ">";
	}
}
