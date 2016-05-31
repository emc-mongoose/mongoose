package com.emc.mongoose.core.impl.item.data;
//
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import com.emc.mongoose.core.impl.item.base.BasicItemNameInput;
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
	private final Input<String> pathInput;
	private final BasicItemNameInput idInput;
	private final ContentSource contentSrc;
	private final SizeInBytes dataSize;
	//
	public NewDataItemInput(
		final Class<T> dataCls, final Input<String> pathInput, final BasicItemNameInput idInput,
		final ContentSource contentSrc, final SizeInBytes dataSize
	) throws NoSuchMethodException, IllegalArgumentException {
		this.itemConstructor = dataCls.getConstructor(
			String.class, String.class, Long.class, Long.class, ContentSource.class
		);
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
	public final T get()
	throws IOException {
		try {
			return itemConstructor.newInstance(
				pathInput.get(), idInput.get(), idInput.getLastValue(), dataSize.get(), contentSrc
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
						pathInput.get(), idInput.get(), idInput.getLastValue(), dataSize.get(),
						contentSrc
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
	public final void close()
	throws IOException {
		if(contentSrc != null) {
			contentSrc.close();
		}
	}
	//
	@Override
	public final String toString() {
		return "newDataItemSrc<" + itemConstructor.getDeclaringClass().getSimpleName() + ">";
	}
}
