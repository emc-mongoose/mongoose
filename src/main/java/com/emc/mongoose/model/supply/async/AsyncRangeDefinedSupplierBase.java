package com.emc.mongoose.model.supply.async;

import com.emc.mongoose.model.exception.OmgDoesNotPerformException;
import com.emc.mongoose.model.supply.BatchSupplier;
import com.emc.mongoose.model.supply.RangeDefinedSupplier;

import com.github.akurilov.fiber4j.FibersExecutor;

import com.github.akurilov.commons.concurrent.InitCallable;
import com.github.akurilov.commons.concurrent.Initializable;
import com.github.akurilov.commons.math.Random;

import java.io.IOException;
import java.util.List;

/**
 * This class is a base class to create inputs that produce values of any types (in specified ranges or not),
 * but their values are intended to be converted to String.
 * @param <T> - type of value that is produced by the input
 */
public abstract class AsyncRangeDefinedSupplierBase<T>
implements Initializable, RangeDefinedSupplier<T> {

	protected final Random rnd;
	private final T minValue;
	private final T range;
	private final BatchSupplier<T> newValueSupplier;

	protected AsyncRangeDefinedSupplierBase(
		final FibersExecutor executor, final long seed, final T minValue, final T maxValue
	) throws OmgDoesNotPerformException {
		this.rnd = new Random(seed);
		this.minValue = minValue;
		this.range = computeRange(minValue, maxValue);
		this.newValueSupplier = new AsyncUpdatingValueSupplier<>(
			executor,
			minValue,
			new InitCallable<T>() {
				//
				@Override
				public boolean isInitialized() {
					return AsyncRangeDefinedSupplierBase.this.isInitialized();
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

	protected abstract T computeRange(final T minValue, final T maxValue);
	protected abstract T rangeValue();
	protected abstract T singleValue();

	/**
	 * An implementation of this method should specify
	 * how to get a String presentation of a clean input-produced value
	 * @param value - a clean input-produced value
	 * @return a String presentation of the value
	 */
	protected abstract String toString(final T value);

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
		return newValueSupplier.get();
	}

	@Override
	public final String get() {
		return toString(value());
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
	public final long skip(final long count) {
		return newValueSupplier.skip(count);
	}

	@Override
	public final void reset() {
		newValueSupplier.reset();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public final void close()
	throws IOException {
		newValueSupplier.close();
	}
}
