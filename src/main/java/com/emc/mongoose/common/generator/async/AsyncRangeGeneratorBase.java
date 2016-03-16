package com.emc.mongoose.common.generator.async;

import com.emc.mongoose.common.generator.RangeGenerator;
import com.emc.mongoose.common.generator.ValueGenerator;
import com.emc.mongoose.common.math.Random;
/**
 * This class is a base class to create generators that produce values of any types (in specified ranges or not),
 * but their values are intended to be converted to String.
 * @param <T> - type of value that is produced by the generator
 */
public abstract class AsyncRangeGeneratorBase<T>
implements Initializable, RangeGenerator<T> {

	protected final Random random = new Random();
	private final T minValue;
	private final T range;
	private final ValueGenerator<T> generator;

	protected AsyncRangeGeneratorBase(final T minValue, final T maxValue) {
		this.minValue = minValue;
		this.range = computeRange(minValue, maxValue);
		this.generator = new AsyncValueGenerator<>(
			minValue,
			new AsyncValueGenerator.InitCallable<T>() {
				//
				@Override
				public boolean isInitialized() {
					return AsyncRangeGeneratorBase.this.isInitialized();
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

	protected AsyncRangeGeneratorBase(final T initialValue) {
		this.minValue = initialValue;
		this.range = null;
		this.generator = new AsyncValueGenerator<>(
			initialValue,
			new AsyncValueGenerator.InitCallable<T>() {
				//
				@Override
				public boolean isInitialized() {
					return AsyncRangeGeneratorBase.this.isInitialized();
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
	 * how to get a String presentation of a clean generator-produced value
	 * @param value - a clean generator-produced value
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
	 * @return - a clean generator-produced value
	 */
	@Override
	public final T value() {
		return generator.get();
	}

	@Override
	public final String get() {
		return stringify(value());
	}

	@Override
	public boolean isInitialized() {
		return true;
	}
}
