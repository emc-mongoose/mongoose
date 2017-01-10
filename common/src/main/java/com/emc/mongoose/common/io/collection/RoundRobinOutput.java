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
	public final boolean put(final T ioTask)
	throws IOException {
		final Output<T> nextOutput = getNextOutput();
		return nextOutput.put(ioTask);
	}

	@Override
	public final int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		Output<T> nextOutput;
		int nextFrom = from;
		for(int i = 0; i < outputs.size(); i ++) {
			nextOutput = getNextOutput();
			nextFrom += nextOutput.put(buffer, nextFrom, to);
			if(nextFrom == to) {
				break;
			}
		}
		return to - nextFrom;
	}

	@Override
	public final int put(final List<T> buffer)
	throws IOException {
		Output<T> nextOutput;
		int from = 0;
		final int to = buffer.size();
		for(int i = 0; i < outputs.size(); i ++) {
			nextOutput = getNextOutput();
			from += nextOutput.put(buffer, from, to);
			if(from == to) {
				break;
			}
		}
		return to - from;
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
