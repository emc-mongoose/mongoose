package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.util.DataItemInput;
//
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
/**
 Created by kurila on 17.06.15.
 Readable collection of the data items.
 */
public class CollectionItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final Collection<T> itemsSrc;
	protected Iterator<T> itemsIter = null;
	//
	public CollectionItemInput(final Collection<T> itemsSrc) {
		this.itemsSrc = itemsSrc;
		try {
			reset();
		} catch(final IOException ignored) {
		}
	}
	/**
	 @return null if the list is empty or no more elements are available
	 @throws java.io.IOException doesn't throw
	 */
	@Override
	public T read()
	throws IOException {
		if(itemsIter == null) {
			reset();
		}
		return itemsIter.hasNext() ? itemsIter.next() : null;
	}
	/**
	 @throws IOException doesn't throw
	 */
	@Override
	public void reset()
	throws IOException{
		itemsIter = itemsSrc.iterator();
	}
	/**
	 Clears the underlying collection
	 @throws IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
		itemsIter = null;
		itemsSrc.clear();
	}
}
