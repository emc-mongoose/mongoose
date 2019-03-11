package com.emc.mongoose.perf;

import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.load.generator.LoadGenerator;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

final class RecyclingAndCountingOutput<T> extends CountingOutput<T> {

	LoadGenerator loadGenerator;

	RecyclingAndCountingOutput(final LongAdder counter) {
		super(counter);
	}

	@Override
	public boolean put(final T item) {
		loadGenerator.recycle((Operation) item);
		return super.put(item);
	}

	@Override
	public int put(final List<T> buffer, final int from, final int to) {
		for (var i = from; i < to; i++) {
			loadGenerator.recycle((Operation) buffer.get(i));
		}
		return super.put(buffer, from, to);
	}

	@Override
	public int put(final List<T> buffer) {
		for (var i = 0; i < buffer.size(); i++) {
			loadGenerator.recycle((Operation) buffer.get(i));
		}
		return super.put(buffer);
	}
}
