package com.emc.mongoose.model.io;
//
import com.emc.mongoose.common.exception.IoFireball;
//
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
/**
 Created by kurila on 10.02.16.
 */
public class BasicValueInput<T>
implements Input<T> {
	//
	protected final T initialValue;
	protected volatile T lastValue = null;
	private Callable<T> updateAction;
	//
	public BasicValueInput(final T initialValue, final Callable<T> updateAction) {
		this.initialValue = initialValue;
		this.updateAction = updateAction;
		try {
			reset();
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	//
	@Override
	public T get()
	throws IoFireball {
		final T prevValue = lastValue;
		try {
			lastValue = updateAction.call();
		} catch(final Exception e) {
			throw new IoFireball("Failed to execute the update action \"{" + updateAction + "\"}");
		}
		return prevValue ;
	}
	//
	@Override
	public int get(final List<T> buffer, final int limit)
	throws IoFireball {
		int count = 0;
		try {
			for(; count < limit; count ++) {
				buffer.add(lastValue);
				lastValue = updateAction.call();
			}
		} catch(final Exception e) {
			throw new IoFireball("Failed to execute the update action \"{" + updateAction + "\"}");
		}
		return count;
	}
	//
	@Override
	public void skip(final long count)
	throws IoFireball {
		try {
			for(int i = 0; i < count; i++) {
				lastValue = updateAction.call();
			}
		} catch(final Exception e) {
			throw new IoFireball("Failed to execute the update action \"{" + updateAction + "\"}");
		}
	}
	//
	@Override
	public void reset()
	throws IOException {
		lastValue = initialValue;
	}
	//
	@Override
	public void close()
	throws IoFireball {
		// just free the memory
		lastValue = null;
		updateAction = null;
	}
}
