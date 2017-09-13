package com.emc.mongoose.api.common.supply;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 Created by kurila on 10.02.16.
 */
public class BasicUpdatingValueSupplier<T>
implements BatchSupplier<T> {
	
	protected final T initialValue;
	protected volatile T lastValue = null;
	private Callable<T> updateAction;
	
	public BasicUpdatingValueSupplier(final T initialValue, final Callable<T> updateAction) {
		this.initialValue = initialValue;
		this.updateAction = updateAction;
		reset();
	}

	@Override
	public T get()  {
		final T prevValue = lastValue;
		try {
			lastValue = updateAction.call();
		} catch(final Exception e) {
			throw new RuntimeException(e);
		}
		return prevValue ;
	}
	
	@Override
	public int get(final List<T> buffer, final int limit) {
		int count = 0;
		try {
			for(; count < limit; count ++) {
				buffer.add(lastValue);
				lastValue = updateAction.call();
			}
		} catch(final Exception e) {
			throw new RuntimeException(e);
		}
		return count;
	}
	
	@Override
	public long skip(final long count) {
		try {
			for(int i = 0; i < count; i++) {
				lastValue = updateAction.call();
			}
		} catch(final Exception e) {
			throw new RuntimeException(e);
		}
		return count;
	}
	
	@Override
	public void reset() {
		lastValue = initialValue;
	}
	
	@Override
	public void close()
	throws IOException {
		lastValue = null;
		updateAction = null;
	}
}
