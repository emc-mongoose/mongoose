package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.model.DataItemDst;
//
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
/**
 The data item output implementation serializing the data items into the specified stream
 */
public abstract class BinItemDst<T extends DataItem>
implements DataItemDst<T> {
	//
	protected final ObjectOutputStream itemsDst;
	//
	protected BinItemDst(final ObjectOutputStream itemsDst) {
		this.itemsDst = itemsDst;
	}
	//
	@Override
	public void put(final T dataItem)
	throws IOException {
		try {
			itemsDst.writeUnshared(dataItem);
		} catch(final ArrayIndexOutOfBoundsException e) {
			e.printStackTrace(System.err);
		}
	}
	//
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		itemsDst.writeUnshared(buffer.subList(from, to));
		return to - from;
	}
	//
	@Override
	public final int put(final List<T> items)
	throws IOException {
		return put(items, 0, items.size());
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
