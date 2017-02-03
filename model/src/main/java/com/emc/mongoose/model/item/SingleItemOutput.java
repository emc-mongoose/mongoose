package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.util.List;

/**
 Created by andrey on 28.04.16.
 */
public class SingleItemOutput<T extends Item>
implements Output<T> {
	
	private volatile T item = null;
	
	@Override
	public boolean put(final T item) {
		this.item = item;
		return true;
	}
	
	@Override
	public final int put(final List<T> buffer, final int from, final int to) {
		item = buffer.size() > 0 ? null : buffer.get(to - 1);
		return 1;
	}
	
	@Override
	public final int put(final List<T> buffer) {
		item = buffer.size() > 0 ? null : buffer.get(buffer.size() - 1);
		return 1;
	}
	
	@Override
	public final Input<T> getInput() {
		return new SingleItemInput<>(item);
	}
	
	@Override
	public final void close() {
		item = null;
	}
}
