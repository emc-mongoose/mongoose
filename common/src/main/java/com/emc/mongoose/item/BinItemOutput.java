package com.emc.mongoose.item;

import com.emc.mongoose.io.Output;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
/**
 The data item output implementation serializing the data items into the specified stream
 */
public abstract class BinItemOutput<T extends Item>
implements Output<T> {
	//
	protected final ObjectOutputStream itemsDst;
	//
	protected BinItemOutput(final ObjectOutputStream itemsDst) {
		this.itemsDst = itemsDst;
	}
	//
	@Override
	public void put(final T item)
	throws IOException {
		try {
			itemsDst.writeUnshared(item);
		} catch(final ArrayIndexOutOfBoundsException e) {
			e.printStackTrace(System.err);
		}
	}
	//
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		itemsDst.writeUnshared(
			buffer
				.subList(from, to)
				.toArray(new Item[to - from])
		);
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
