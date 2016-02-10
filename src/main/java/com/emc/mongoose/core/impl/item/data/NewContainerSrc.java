package com.emc.mongoose.core.impl.item.data;

import com.emc.mongoose.common.conf.ItemIdGenerator;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by gusakk on 21.10.15.
 */
public class NewContainerSrc<T extends Container>
implements ItemSrc<T> {
	//
	private final Constructor<T> itemConstructor;
	private final ItemIdGenerator namingScheme;
	//
	private T lastItem = null;
	//
	public NewContainerSrc(
		final Class<T> dataCls, final ItemIdGenerator namingScheme
	) throws NoSuchMethodException, IllegalArgumentException {
		itemConstructor = dataCls.getConstructor(String.class);
		this.namingScheme = namingScheme;
	}
	//
	@Override
	public final T get()
	throws IOException {
		try {
			return itemConstructor.newInstance(
				Long.toString(namingScheme.get(), DataItem.ID_RADIX)
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
						Long.toString(namingScheme.get(), DataItem.ID_RADIX)
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
		return "newContainerSrc<" + itemConstructor.getDeclaringClass().getSimpleName() + ">";
	}

}
