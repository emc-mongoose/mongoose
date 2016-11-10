package com.emc.mongoose.common.io.range;

import com.emc.mongoose.common.concurrent.InitCallable;
import com.emc.mongoose.common.concurrent.Initializable;
import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.math.Random;
import com.emc.mongoose.common.io.AsyncValueInput;
import com.emc.mongoose.common.io.Input;

import java.io.IOException;
import java.util.List;

/**
 * This class is a base class to create inputs that produce values of any types (in specified ranges or not),
 * but their values are intended to be converted to String.
 * @param <T> - type of value that is produced by the input
 */
public abstract class AsyncRangeInputBase<T>
implements Initializable, RangeInput<T> {

	protected final Random random = new Random();
	private final T minValue;
	private final T range;
	private final Input<T> input;

	protected AsyncRangeInputBase(final T minValue, final T maxValue)
	throws OmgDoesNotPerformException {
		this.minValue = minValue;
		this.range = computeRange(minValue, maxValue);
		this.input = new AsyncValueInput<>(
			minValue,
			new InitCallable<T>() {
				//
				@Override
				public boolean isInitialized() {
					return AsyncRangeInputBase.this.isInitialized();
				}
				//
				@Override
				public T call()
				throws Exception {
					return rangeValue();
				}
			}
		);
	}

	protected AsyncRangeInputBase(final T initialValue)
	throws OmgDoesNotPerformException {
		this.minValue = initialValue;
		this.range = null;
		this.input = new AsyncValueInput<>(
			initialValue,
			new InitCallable<T>() {
				//
				@Override
				public boolean isInitialized() {
					return AsyncRangeInputBase.this.isInitialized();
				}
				//
				@Override
				public T call() throws Exception {
					return singleValue();
				}
			}
		);
	}

	protected abstract T computeRange(final T minValue, final T maxValue);
	protected abstract T rangeValue();
	protected abstract T singleValue();

	/**
	 * An implementation of this method should specify
	 * how to get a String presentation of a clean input-produced value
	 * @param value - a clean input-produced value
	 * @return a String presentation of the value
	 */
	protected abstract String stringify(final T value);

	protected final T minValue() {
		return minValue;
	}

	protected final T range() {
		return range;
	}

	/**
	 *
	 * @return - a clean input-produced value
	 */
	@Override
	public final T value() {
		try {
			return input.get();
		} catch(final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final String get() {
		return stringify(value());
	}

	@Override
	public final int get(final List<String> buffer, final int limit) {
		int count = 0;
		for(; count < limit; count ++) {
			buffer.add(get());
		}
		return count;
	}

	@Override
	public final void skip(final long count)
	throws IOException {
		input.skip(count);
	}

	@Override
	public final void reset()
	throws IOException {
		input.reset();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public final void close()
	throws IOException {
		input.close();
	}
}
