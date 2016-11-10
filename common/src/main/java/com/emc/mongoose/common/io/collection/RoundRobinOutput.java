package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by andrey on 06.11.16.
 */
public final class RoundRobinOutput<T>
implements Output<T> {

	private final List<? extends Output<T>> outputs;
	private final int count;
	private final AtomicLong rrc = new AtomicLong(0);

	public RoundRobinOutput(final List<? extends Output<T>> outputs) {
		this.outputs = outputs;
		this.count = outputs.size();
	}

	private Output<T> getNextOutput() {
		if(count > 1) {
			return outputs.get((int) (rrc.incrementAndGet() % count));
		} else {
			return outputs.get(0);
		}
	}

	@Override
	public final void put(final T ioTask)
	throws IOException {
		final Output<T> nextOutput = getNextOutput();
		nextOutput.put(ioTask);
	}

	@Override
	public final int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		final Output<T> nextOutput = getNextOutput();
		return nextOutput.put(buffer, from, to);
	}

	@Override
	public final int put(final List<T> buffer)
	throws IOException {
		final Output<T> nextOutput = getNextOutput();
		return nextOutput.put(buffer, 0, buffer.size());
	}

	@Override
	public final Input<T> getInput()
	throws IOException {
		return null;
	}

	@Override
	public final void close()
	throws IOException {
	}
}
