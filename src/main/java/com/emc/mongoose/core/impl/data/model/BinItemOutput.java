package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.model.DataItemOutput;
//
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
/**
 The data item output implementation serializing the data items into the specified stream
 */
public abstract class BinItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final ObjectOutputStream itemsDst;
	//
	protected BinItemOutput(final ObjectOutputStream itemsDst) {
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
	public int write(final List<T> buffer)
	throws IOException {
		itemsDst.writeUnshared(buffer);
		return buffer.size();
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsDst.close();
	}
	//
	@Override
	public String toString() {
		return "binItemOutput<" + itemsDst + ">";
	}
}
