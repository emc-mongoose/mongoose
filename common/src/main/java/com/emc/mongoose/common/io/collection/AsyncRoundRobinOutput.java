package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
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

	private final class SingleOutputTask<T>
	extends FutureTaskBase<T> {
		
		private final AsyncRoundRobinOutput<T> rrcOutput;
		private final T ioTask;
		
		public SingleOutputTask(final AsyncRoundRobinOutput<T> rrcOutput, final T ioTask) {
			this.rrcOutput = rrcOutput;
			this.ioTask = ioTask;
		}
		
		@Override
		public final void run() {
			try {
				while(!rrcOutput.getNextOutput().put(ioTask)) {
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
		try {
			outputExecutor.execute(new SingleOutputTask<>(this, ioTask));
			return true;
		} catch(final RejectedExecutionException e) {
			return false;
		}
	}
	
	private final static class BatchOutputTask<T>
	extends FutureTaskBase<Integer> {
		
		private final AsyncRoundRobinOutput<T> rrcOutput;
		private final List<T> buff;
		private int from;
		private final int to;
		
		public BatchOutputTask(
			final AsyncRoundRobinOutput<T> rrcOutput, final List<T> buff, final int from,
			final int to
		) {
			this.rrcOutput = rrcOutput;
			this.buff = buff;
			this.from = from;
			this.to = to;
		}
		
		@Override
		public final void run() {
			try {
				while(from < to) {
					from += rrcOutput.getNextOutput().put(buff, from, to);
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
				try {
					outputExecutor.execute(
						new BatchOutputTask<>(
							this, buffer, from + i * nPerOutput, from + nPerOutput + i * nPerOutput
						)
					);
				} catch(final RejectedExecutionException e) {
					return i * nPerOutput;
				}
			}
			final int nTail = n - nPerOutput * outputsCount;
			if(nTail > 0) {
				try {
					outputExecutor.execute(
						new BatchOutputTask<>(this, buffer, to - nTail, to)
					);
				} catch(final RejectedExecutionException e) {
					return n - nTail;
				}
			}
			return n;
		} else {
			for(int i = from; i < to; i ++) {
				try {
					outputExecutor.execute(new SingleOutputTask<>(this, buffer.get(i)));
				} catch(final RejectedExecutionException e) {
					return to - i;
				}
			}
			return n;
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
		outputExecutor.shutdownNow();
		outputs.clear();
	}
}
