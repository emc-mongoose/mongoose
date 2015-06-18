package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.util.DataItemOutput;
//
import java.io.IOException;
import java.io.ObjectOutputStream;
/**
 Created by kurila on 18.06.15.
 */
public abstract class SerializingItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final ObjectOutputStream itemsDst;
	//
	protected SerializingItemOutput(final ObjectOutputStream itemsDst) {
		this.itemsDst = itemsDst;
	}
	//
	@Override
	public void write(final T dataItem)
	throws IOException {
		itemsDst.writeUnshared(dataItem);
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsDst.close();
	}
}
