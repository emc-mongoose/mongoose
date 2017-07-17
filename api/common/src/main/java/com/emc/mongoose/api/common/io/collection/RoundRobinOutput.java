package com.emc.mongoose.api.common.io.collection;

import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.io.Output;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by andrey on 06.11.16.
 @deprecated Use RoundRobinOutputsTransferSvcTask instead
 */
@Deprecated
public final class RoundRobinOutput<T>
implements Output<T> {
	
	private final List<? extends Output<T>> outputs;
	private final int outputsCount;
	private final AtomicLong rrc = new AtomicLong(0);
	
	public RoundRobinOutput(final List<? extends Output<T>> outputs) {
		this.outputs = outputs;
		this.outputsCount = outputs.size();
	}
	
	private Output<T> getNextOutput() {
		if(outputsCount > 1) {
			return outputs.get((int) (rrc.incrementAndGet() % outputsCount));
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
		final int n = to - from;
		if(n > outputsCount) {
			final int nPerOutput = n / outputsCount;
			int nextFrom = from;
			for(int i = 0; i < outputsCount; i ++) {
				nextOutput = getNextOutput();
				nextFrom += nextOutput.put(buffer, nextFrom, nextFrom + nPerOutput);
			}
			if(nextFrom < to) {
				nextOutput = getNextOutput();
				nextFrom += nextOutput.put(buffer, nextFrom, to);
			}
			return nextFrom - from;
		} else {
			for(int i = from; i < to; i ++) {
				nextOutput = getNextOutput();
				if(!nextOutput.put(buffer.get(i))) {
					return i - from;
				}
			}
			return to - from;
		}
	}
	
	@Override
	public final int put(final List<T> buffer)
	throws IOException {
		Output<T> nextOutput;
		final int n = buffer.size();
		if(n > outputsCount) {
			final int nPerOutput = n / outputsCount;
			int nextFrom = 0;
			for(int i = 0; i < outputsCount; i ++) {
				nextOutput = getNextOutput();
				nextFrom += nextOutput.put(buffer, nextFrom, nextFrom + nPerOutput);
			}
			if(nextFrom < n) {
				nextOutput = getNextOutput();
				nextFrom += nextOutput.put(buffer, nextFrom, n);
			}
			return nextFrom;
		} else {
			for(int i = 0; i < n; i ++) {
				nextOutput = getNextOutput();
				if(!nextOutput.put(buffer.get(i))) {
					return i;
				}
			}
			return n;
		}
	}
	
	@Override
	public final Input<T> getInput()
	throws IOException {
		throw new AssertionError("Shouldn't be invoked");
	}
	
	@Override
	public final void close()
	throws IOException {
		outputs.clear();
	}
}
