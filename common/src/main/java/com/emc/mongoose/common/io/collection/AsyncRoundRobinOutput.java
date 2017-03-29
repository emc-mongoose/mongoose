package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 06.11.16.
 */
public final class AsyncRoundRobinOutput<T>
implements Output<T> {

	private final List<? extends Output<T>> outputs;
	private final int outputsCount;
	private final AtomicLong rrc = new AtomicLong(0);
	private final ExecutorService outputExecutor;

	public AsyncRoundRobinOutput(
		final List<? extends Output<T>> outputs, final int queueSizeLimit
	) {
		this.outputs = outputs;
		this.outputsCount = outputs.size();
		this.outputExecutor = new ThreadPoolExecutor(
			ThreadUtil.getHardwareConcurrencyLevel(), ThreadUtil.getHardwareConcurrencyLevel(),
			0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSizeLimit)
		);
	}

	private Output<T> getNextOutput() {
		if(outputsCount > 1) {
			return outputs.get((int) (rrc.incrementAndGet() % outputsCount));
		} else {
			return outputs.get(0);
		}
	}

	private final class SingleOutputTask
	extends FutureTaskBase<T> {
		
		private final T ioTask;
		
		public SingleOutputTask(final T ioTask) {
			this.ioTask = ioTask;
		}
		
		@Override
		public final void run() {
			try {
				while(!getNextOutput().put(ioTask)) {
					LockSupport.parkNanos(1);
				}
				set(ioTask);
			} catch(final IOException e) {
				setException(e);
			}
		}
	}
	
	@Override
	public final boolean put(final T ioTask)
	throws IOException {
		outputExecutor.execute(new SingleOutputTask(ioTask));
		return true;
	}
	
	private final class BatchOutputTask
	extends FutureTaskBase<Integer> {
		
		private final List<T> buff;
		private int from;
		private final int to;
		
		public BatchOutputTask(final List<T> buff, final int from, final int to) {
			this.buff = buff;
			this.from = from;
			this.to = to;
		}
		
		@Override
		public final void run() {
			try {
				while(from < to) {
					from += getNextOutput().put(buff, from, to);
				}
				set(to - from);
			} catch(final IOException e) {
				setException(e);
			}
		}
	}
	
	@Override
	public final int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		final int n = to - from;
		if(n > outputsCount) {
			final int nPerOutput = n / outputsCount;
			for(int i = 0; i < outputsCount; i ++) {
				outputExecutor.execute(
					new BatchOutputTask(
						buffer, from + i * nPerOutput, from + nPerOutput + i * nPerOutput
					)
				);
			}
			final int nTail = n - nPerOutput * outputsCount;
			if(nTail > 0) {
				outputExecutor.execute(
					new BatchOutputTask(buffer, to - nTail, to)
				);
			}
			return to - from;
		} else {
			for(int i = from; i < to; i ++) {
				outputExecutor.execute(new SingleOutputTask(buffer.get(i)));
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
		outputExecutor.shutdownNow();
		outputs.clear();
	}
}
