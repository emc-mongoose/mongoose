package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.util.DataItemOutput;
//
import java.io.IOException;
import java.util.Collection;
/**
 Created by kurila on 18.06.15.
 Writable collection of the data items.
 */
public class CollectionItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final Collection<T> itemsDst;
	//
	public CollectionItemOutput(final Collection<T> itemsDst) {
		this.itemsDst = itemsDst;
	}
	/**
	 @param dataItem the data item to write
	 @throws IOException if the destination collection fails to add the data item
	 (due to capacity reasons for example)
	 */
	@Override
	public void write(final T dataItem)
	throws IOException {
		if(!itemsDst.add(dataItem)) {
			throw new IOException("Failed to add the data item to the destination collection");
		}
	}
	/**
	 @return the corresponding input
	 @throws IOException doesn't throw
	 */
	@Override
	public CollectionItemInput<T> getInput()
	throws IOException {
		return new CollectionItemInput<>(itemsDst);
	}
	/**
	 Clear the underlying collection
	 @throws java.io.IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
		itemsDst.clear();
	}
}
