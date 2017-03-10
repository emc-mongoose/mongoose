package com.emc.mongoose.common.supply;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 Created by kurila on 10.03.17.
 */
public final class CircularSetSupplier<T>
implements BatchSupplier<T> {
	
	private final Set<T> values;
	private Iterator<T> it;
	
	public CircularSetSupplier(final Set<T> values) {
		this.values = values;
		it = values.iterator();
	}
	
	@Override
	public final T get() {
		if(!it.hasNext()) {
			it = values.iterator();
		}
		return it.next();
	}
	
	@Override
	public final int get(final List<T> buffer, final int limit) {
		for(int i = 0; i < limit; i ++) {
			if(!it.hasNext()) {
				it = values.iterator();
			}
			buffer.add(it.next());
		}
		return limit;
	}
	
	@Override
	public final long skip(final long count) {
		for(long i = 0; i < count; i ++) {
			if(!it.hasNext()) {
				it = values.iterator();
			}
			it.next();
		}
		return count;
	}
	
	@Override
	public final void reset() {
		it = values.iterator();
	}
	
	@Override
	public final void close() {
		values.clear();
		it = null;
	}
}
