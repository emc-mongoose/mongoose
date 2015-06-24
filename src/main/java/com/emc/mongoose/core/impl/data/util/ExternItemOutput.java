package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.util.DataItemOutput;
//
import java.io.IOException;
import java.io.ObjectOutputStream;
/**
 The data item output implementation serializing the data items into the specified stream
 */
public abstract class ExternItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final ObjectOutputStream itemsDst;
	//
	protected ExternItemOutput(final ObjectOutputStream itemsDst) {
		this.itemsDst = itemsDst;
	}
	//
	@Override
	public void write(final T dataItem)
	throws IOException {
		try {
			itemsDst.writeUnshared(dataItem);
		} catch(final ArrayIndexOutOfBoundsException e) {
			e.printStackTrace(System.err);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsDst.close();
	}
}
