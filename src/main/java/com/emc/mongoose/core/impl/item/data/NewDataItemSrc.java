package com.emc.mongoose.core.impl.item.data;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicItemIdGenerator;
import com.emc.mongoose.common.conf.ItemIdGenerator;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.base.ItemSrc;
//
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 24.07.15.
 */
public final class NewDataItemSrc<T extends DataItem>
implements ItemSrc<T> {
	//
	private final Constructor<T> itemConstructor;
	private final ItemIdGenerator itemIdGenerator;
	private final ContentSource contentSrc;
	private final SizeInBytes dataSize;
	private T lastItem = null;
	//
	public NewDataItemSrc(
		final Class<T> dataCls, final AppConfig.ItemNamingType namingType,
		final ContentSource contentSrc, final SizeInBytes dataSize
	) throws NoSuchMethodException, IllegalArgumentException {
		this.itemConstructor = dataCls.getConstructor(Long.class, Long.class, ContentSource.class);
		this.itemIdGenerator = new BasicItemIdGenerator(namingType);
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
				itemIdGenerator.get(), dataSize.get(), contentSrc
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
						itemIdGenerator.get(), dataSize.get(), contentSrc
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
