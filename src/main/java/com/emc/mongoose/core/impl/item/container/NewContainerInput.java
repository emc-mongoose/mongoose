package com.emc.mongoose.core.impl.item.container;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.container.Container;
//
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
/**
 * Created by gusakk on 21.10.15.
 */
public class NewContainerInput<T extends Container>
implements Input<T> {
	//
	private final Constructor<T> itemConstructor;
	private final Input<String> idInput;
	private final String path;
	//
	public NewContainerInput(final Class<T> itemCls, final Input<String> idInput, final String path)
	throws NoSuchMethodException, IllegalArgumentException {
		itemConstructor = itemCls.getConstructor(String.class, String.class);
		this.idInput = idInput;
		this.path = path;
	}
	//
	@Override
	public final T get()
	throws IOException {
		try {
			return itemConstructor.newInstance(path, idInput.get());
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
				buffer.add(itemConstructor.newInstance(path, idInput.get()));
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
		return "newContainerSrc<" + itemConstructor.getDeclaringClass().getSimpleName() + ">";
	}

}
