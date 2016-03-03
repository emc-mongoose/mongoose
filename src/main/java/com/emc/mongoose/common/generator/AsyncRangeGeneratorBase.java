package com.emc.mongoose.common.generator;

import java.util.Random;
import com.emc.mongoose.common.generator.AsyncValueGenerator.InitCallable;

/**
 * This class is a base class to create generators that produce values of any types (in specified ranges or not), but their values are intended
 * to be converted to String.
 * @param <T> - type of value that is produced by the generator
 */
public abstract class AsyncRangeGeneratorBase<T>
implements Initializable, ValueGenerator<String> {

	protected final Random random = new Random();
	private final T minValue;
	private final T range;
	private final AsyncValueGenerator<T> generator;

	protected AsyncRangeGeneratorBase(final T minValue, final T maxValue) {
		this.minValue = minValue;
		this.range = computeRange(minValue, maxValue);
		this.generator = new AsyncValueGenerator<>(
			minValue,
			new InitCallable<T>() {
				//
				@Override
				public boolean isInitialized() {
					return AsyncRangeGeneratorBase.this.isInitialized();
				}
				//
				@Override
				public T call() throws Exception {
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
			new InitCallable<T>() {
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

	protected T minValue() {
		return minValue;
	}

	protected T range() {
		return range;
	}

	/**
	 *
	 * @return - a clean generator-produced value
	 */
	protected T value() {
		return generator.get();
	}

	@Override
	public String get() {
		return stringify(value());
	}

}
