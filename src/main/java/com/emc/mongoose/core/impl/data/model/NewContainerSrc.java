package com.emc.mongoose.core.impl.data.model;

import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by gusakk on 21.10.15.
 */
public class NewContainerSrc<T extends Container>
implements ItemSrc<T> {
	//
	private static AtomicLong
		LAST_OFFSET = new AtomicLong(
		Math.abs(
			Long.reverse(System.currentTimeMillis()) ^
			Long.reverseBytes(System.nanoTime()) ^
			ServiceUtil.getHostAddrCode()
		)
	);
	//
	private final Constructor<T> itemConstructor;
	private T lastItem = null;
	//
	public NewContainerSrc(
		final Class<T> dataCls
	) throws NoSuchMethodException, IllegalArgumentException {
		itemConstructor = dataCls.getConstructor(String.class);
	}
	//
	@Override
	public final T get()
	throws IOException {
		try {
			return itemConstructor.newInstance(nextName(LAST_OFFSET));
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
				buffer.add(itemConstructor.newInstance(nextName(LAST_OFFSET)));
			}
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
		}
		return maxCount;
	}
	//
	public String nextName(final AtomicLong lastOffset) {
		long newOffset = lastOffset.getAndSet(
			Math.abs(
				ContentSourceBase.nextWord(lastOffset.get()) ^ System.nanoTime()
			)
		);
		//
		return Long.toString(newOffset, DataItem.ID_RADIX);
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
