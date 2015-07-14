package com.emc.mongoose.core.impl.data;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
//
import com.emc.mongoose.core.api.data.model.GenericContainer;
//
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
/**
 The implementation should have a state representing the actual position in the container listing
 */
public abstract class GenericContainerItemInputBase<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final GenericContainer<T> container;
	protected final String nodeAddr;
	protected final Constructor<T> itemConstructor;
	protected final List<T> listPageBuffer = new ArrayList<>(RequestConfig.PAGE_SIZE);
	protected ListIterator<T> listPageIter = null;
	//
	protected GenericContainerItemInputBase(
		final GenericContainer<T> container, final String nodeAddr, final Class<T> itemCls
	) throws IllegalStateException {
		this.container = container;
		this.nodeAddr = nodeAddr;
		try {
			this.itemConstructor = itemCls.getConstructor(
				String.class, Long.class, Long.class
			);
		} catch(final NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}
	/**
	 The method should fill the listPageBuffer and return its list iterator
	 @return the list iterator for the buffered items list
	 @throws EOFException if no more items is available from the storage side
	 @throws IOException
	 */
	protected abstract ListIterator<T> getNextPageIterator()
	throws EOFException, IOException;
	//
	@Override
	public final T read()
	throws EOFException, IOException {
		if(listPageIter == null || !listPageIter.hasNext()) {
			listPageIter = getNextPageIterator();
		}
		return listPageIter.next();
	}
	//
	@Override
	public final int read(final List<T> buffer)
	throws IOException {
		if(listPageIter == null || !listPageIter.hasNext()) {
			listPageIter = getNextPageIterator();
		}
		int n = buffer.size(), m = listPageIter.nextIndex();
		buffer.addAll(listPageBuffer.subList(m, listPageBuffer.size()));
		n = buffer.size() - n;
		listPageIter = listPageBuffer.listIterator(n + m);
		return n;
	}
	/**
	 Read the items from the beginning of the container listing
	 @throws IOException
	 */
	@Override
	public void reset()
	throws IOException {
		listPageBuffer.clear();
		listPageIter = null;
	}
	/**
	 The default implementation does nothing
	 @throws IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
	}
}
