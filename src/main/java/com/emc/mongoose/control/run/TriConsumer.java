package com.emc.mongoose.control.run;

@FunctionalInterface
public interface TriConsumer<T, U, V> {

	void accept(final T t, final U u, final V v);
}
