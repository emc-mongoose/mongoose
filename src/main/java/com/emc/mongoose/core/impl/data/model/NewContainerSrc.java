package com.emc.mongoose.core.impl.data.model;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.data.model.ItemSrc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by gusakk on 21.10.15.
 */
public class NewContainerSrc<T extends Container>
implements ItemSrc<T> {
	//
	private final Constructor<T> itemConstructor;
	private T lastItem = null;
	//
	public NewContainerSrc(
		final Class<T> dataCls
	) throws NoSuchMethodException, IllegalArgumentException {
		this.itemConstructor = dataCls.getConstructor(String.class);

	}
	//
	@Override
	public final T get()
			throws IOException {
		try {
			itemConstructor.newInstance();
			//return itemConstructor.newInstance(nextSize(), contentSrc);
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
		}
		return null;
	}
	//
	@Override
	public int get(final List<T> buffer, final int maxCount)
			throws IOException {
		/*try {
			for(int i = 0; i < maxCount; i ++) {
				buffer.add(itemConstructor.newInstance(nextSize(), contentSrc));
			}
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
		}*/
		return maxCount;
	}
	//
	private String nextName() {
		return Constants.MONGOOSE_PREFIX + LogUtil.FMT_DT.format(instance.getTime());
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
		return "newDataItemInput<" + itemConstructor.getDeclaringClass().getSimpleName() + ">";
	}

}
