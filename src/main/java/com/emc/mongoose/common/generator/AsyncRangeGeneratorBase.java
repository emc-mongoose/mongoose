package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.generator.AsyncValueGenerator.InitCallable;

import java.util.Random;

public abstract class AsyncRangeGeneratorBase<T>
implements Initializable, ValueGenerator<T> {

	protected final Random random = new Random();
	private final T minValue;
	private final T range;
	private final AsyncValueGenerator<T> generator;

	public AsyncRangeGeneratorBase(final T minValue, final T maxValue) {
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

	public AsyncRangeGeneratorBase(final T initialValue) {
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

	public T minValue() {
		return minValue;
	}

	public T range() {
		return range;
	}

	@Override
	public T get() {
		return generator.get();
	}

}
