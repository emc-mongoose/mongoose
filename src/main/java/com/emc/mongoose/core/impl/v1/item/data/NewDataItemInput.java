package com.emc.mongoose.core.impl.v1.item.data;
//
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.core.api.v1.item.data.ContentSource;
//
import com.emc.mongoose.core.impl.v1.item.base.BasicItemNameInput;
//
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
/**
 Created by kurila on 24.07.15.
 */
public final class NewDataItemInput<T extends DataItem>
implements Input<T> {
	//
	private final Constructor<T> itemConstructor;
	private final BasicItemNameInput idInput;
	private final ContentSource contentSrc;
	private final SizeInBytes dataSize;
	private T lastItem = null;
	//
	public NewDataItemInput(
		final Class<T> dataCls, final BasicItemNameInput idInput,
		final ContentSource contentSrc, final SizeInBytes dataSize
	) throws NoSuchMethodException, IllegalArgumentException {
		this.itemConstructor = dataCls.getConstructor(
			String.class, Long.class, Long.class, ContentSource.class
		);
		this.idInput = idInput;
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
				idInput.get(), idInput.getLastValue(), dataSize.get(), contentSrc
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
						idInput.get(), idInput.getLastValue(), dataSize.get(), contentSrc
					)
				);
			}
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
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
	public final void close() {
	}
	//
	@Override
	public final String toString() {
		return "newDataItemSrc<" + itemConstructor.getDeclaringClass().getSimpleName() + ">";
	}
}
