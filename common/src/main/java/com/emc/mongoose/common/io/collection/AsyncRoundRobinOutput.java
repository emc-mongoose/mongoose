package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.concurrent.OutputWrapperSvcTask;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by andrey on 06.11.16.
 */
public final class AsyncRoundRobinOutput<T, O extends Output<T>>
implements Output<T> {

	private final int outputsCount;
	private final AtomicLong rrc = new AtomicLong(0);
	private final Set<Runnable> svcTasks;
	private final List<OutputWrapperSvcTask<T, O>> outputTasks;

	public AsyncRoundRobinOutput(final List<O> outputs, final Set<Runnable> svcTasks) {
		this.outputsCount = outputs.size();
		this.svcTasks = svcTasks;
		this.outputTasks = new ArrayList<>(outputsCount);
		OutputWrapperSvcTask<T, O> nextOutputTask;
		for(int i = 0; i < outputsCount; i++) {
			nextOutputTask = new OutputWrapperSvcTask<>(outputs.get(i), BATCH_SIZE);
			outputTasks.add(nextOutputTask);
			svcTasks.add(nextOutputTask);
		}
	}

	private OutputWrapperSvcTask<T, O> getOutputTask() {
		if(outputsCount > 1) {
			return outputTasks.get((int) (rrc.incrementAndGet() % outputsCount));
		} else {
			return outputTasks.get(0);
		}
	}

	@Override
	public final boolean put(final T ioTask)
	throws IOException {
		return getOutputTask().put(ioTask);
	}

	@Override
	public final int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		OutputWrapperSvcTask<T, O> outputTask;
		final int n = to - from;
		if(n > outputsCount) {
			final int nPerOutput = n / outputsCount;
			int nextFrom = from;
			for(int i = 0; i < outputsCount; i ++) {
				outputTask = getOutputTask();
				nextFrom += outputTask.put(buffer, nextFrom, nextFrom + nPerOutput);
			}
			if(nextFrom < to) {
				outputTask = getOutputTask();
				nextFrom += outputTask.put(buffer, nextFrom, to);
			}
			return nextFrom - from;
		} else {
			for(int i = from; i < to; i ++) {
				outputTask = getOutputTask();
				if(! outputTask.put(buffer.get(i))) {
					return i - from;
				}
			}
			return to - from;
		}
	}

	@Override
	public final int put(final List<T> buffer)
	throws IOException {
		return put(buffer, 0, buffer.size());
	}

	@Override
	public final Input<T> getInput()
	throws IOException {
		throw new AssertionError("Shouldn't be invoked");
	}

	@Override
	public final void close()
	throws IOException {
		svcTasks.removeAll(outputTasks);
		for(final OutputWrapperSvcTask<T, O> outputTask : outputTasks) {
			outputTask.close();
		}
		outputTasks.clear();
	}
}
