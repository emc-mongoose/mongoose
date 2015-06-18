package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.util.client.api.DataItemInput;
//
import java.util.ArrayList;
import java.util.Iterator;
/**
 Created by kurila on 17.06.15.
 Readable list of the data items.
 */
public class ListItemInput<T extends DataItem>
extends ArrayList<T>
implements DataItemInput<T> {
	//
	protected final ThreadLocal<Iterator<T>> thrLocalIter = new ThreadLocal<>();
	/**
	 The method implementation is thread safe and circular.
	 @return null if the list is empty, the next data item in the list otherwise
	 */
	@Override
	public T read() {
		Iterator<T> iter = thrLocalIter.get();
		if(iter == null || !iter.hasNext()) {
			iter = iterator();
			thrLocalIter.set(iter);
		}
		return iter.next();
	}
}
