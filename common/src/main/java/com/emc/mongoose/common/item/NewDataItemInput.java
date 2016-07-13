package com.emc.mongoose.common.item;

import com.emc.mongoose.common.data.ContentSource;
import com.emc.mongoose.common.data.ContentSourceUtil;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.util.SizeInBytes;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public final class NewDataItemInput<D extends DataItem>
implements Input<D> {
	//
	private final Constructor<D> itemConstructor;
	private final Input<String> pathInput;
	private final BasicItemNameInput idInput;
	private final ContentSource contentSrc;
	private final SizeInBytes dataSize;
	//
	public NewDataItemInput(
		final Class<D> dataCls, final Input<String> pathInput, final BasicItemNameInput idInput,
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
	private final static ThreadLocal<Object[]> ARG_BUFF = new ThreadLocal<Object[]>() {
		@Override
		protected final Object[] initialValue() {
			return new Object[5];
		}
	};
	//
	@Override
	public final D get()
	throws IOException {
		final Object[] argBuff = ARG_BUFF.get();
		argBuff[0] = pathInput.get();
		argBuff[1] = idInput.get();
		argBuff[2] = idInput.getLastValue();
		argBuff[3] = dataSize.get();
		argBuff[4] = contentSrc;
		try {
			return itemConstructor.newInstance(argBuff);
			//	pathInput.get(), idInput.get(), idInput.getLastValue(), dataSize.get(), contentSrc
			//);
		} catch(final InstantiationException|IllegalAccessException|InvocationTargetException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public int get(final List<D> buffer, final int maxCount)
	throws IOException {
		final Object[] argBuff = ARG_BUFF.get();
		try {
			for(int i = 0; i < maxCount; i ++) {
				argBuff[0] = pathInput.get();
				argBuff[1] = idInput.get();
				argBuff[2] = idInput.getLastValue();
				argBuff[3] = dataSize.get();
				argBuff[4] = contentSrc;
				buffer.add(
					itemConstructor.newInstance(argBuff)
					//	pathInput.get(), idInput.get(), idInput.getLastValue(), dataSize.get(),
					//	contentSrc
					//)
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
