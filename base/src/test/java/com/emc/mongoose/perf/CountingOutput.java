package com.emc.mongoose.perf;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

class CountingOutput<T> implements Output<T> {

	private final LongAdder counter;

	CountingOutput(final LongAdder counter) {
		this.counter = counter;
	}

	@Override
	public boolean put(final T item) {
		counter.increment();
		return true;
	}

	@Override
	public int put(final List<T> buffer, final int from, final int to) {
		counter.add(to - from);
		return to - from;
	}

	@Override
	public int put(final List<T> buffer) {
		counter.add(buffer.size());
		return buffer.size();
	}

	@Override
	public Input<T> getInput() {
		return null;
	}

	@Override
	public void close() throws Exception {}
}
