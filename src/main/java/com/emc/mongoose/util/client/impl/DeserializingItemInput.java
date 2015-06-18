package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.util.client.api.DataItemInput;

import java.io.IOException;
import java.io.ObjectInputStream;
/**
 Created by kurila on 18.06.15.
 */
public class DeserializingItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final ObjectInputStream itemsSrc;
	//
	public DeserializingItemInput(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public T read()
	throws IOException {
		T nextItem = null;
		try {
			nextItem = (T) itemsSrc.readUnshared();
		} catch(final ClassNotFoundException e) {
			throw new IOException(e);
		}
		return nextItem;
	}
	//
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsSrc.close();
	}
}
