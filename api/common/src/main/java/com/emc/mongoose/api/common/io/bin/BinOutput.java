package com.emc.mongoose.api.common.io.bin;

import com.emc.mongoose.api.common.io.Output;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 The data item output implementation serializing something into the specified stream
 */
public abstract class BinOutput<T>
implements Output<T> {
	
	protected final ObjectOutputStream output;
	
	protected BinOutput(final ObjectOutputStream output) {
		this.output = output;
	}
	
	@Override
	public boolean put(final T item)
	throws IOException {
		output.writeUnshared(item);
		return true;
	}
	
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		output.writeUnshared(
			buffer
				.subList(from, to)
				.toArray(new Object[to - from])
		);
		return to - from;
	}
	
	@Override
	public final int put(final List<T> items)
	throws IOException {
		return put(items, 0, items.size());
	}

	@Override
	public void close()
	throws IOException {
		output.close();
	}
	
	@Override
	public String toString() {
		return "binOutput<" + output + ">";
	}
}
