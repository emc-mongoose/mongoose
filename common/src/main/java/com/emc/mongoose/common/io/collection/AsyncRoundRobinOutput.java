package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.concurrent.BatchQueueOutputTask;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by andrey on 06.11.16.
 */
public final class AsyncRoundRobinOutput<T>
implements Output<T> {

	private final List<? extends Output<T>> outputs;
	private final List<BlockingQueue<T>> outputQueues;
	private final ExecutorService outputExecutor;
	private final int outputsCount;
	private final AtomicLong rrc = new AtomicLong(0);

	public AsyncRoundRobinOutput(
		final List<? extends Output<T>> outputs, final int queueSizeLimit
	) {
		this.outputs = outputs;
		this.outputsCount = outputs.size();
		this.outputQueues = new ArrayList<>(outputsCount);
		this.outputExecutor = Executors.newFixedThreadPool(outputsCount);
		for(int i = 0; i < outputsCount; i ++) {
			final BlockingQueue<T> outputQueue = new ArrayBlockingQueue<>(queueSizeLimit);
			outputQueues.add(outputQueue);
			outputExecutor.submit(new BatchQueueOutputTask<>(outputQueue, outputs.get(i)));
		}
	}

	@Override
	public final boolean put(final T ioTask)
	throws IOException {
		if(outputExecutor.isShutdown()) {
			throw new EOFException();
		}
		if(outputsCount == 1) {
			return outputQueues.get(0).offer(ioTask);
		} else {
			return outputQueues.get((int) (rrc.incrementAndGet() % outputsCount)).offer(ioTask);
		}
	}

	@Override
	public final int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		if(outputExecutor.isShutdown()) {
			throw new EOFException();
		}
		if(outputsCount == 1) {
			final BlockingQueue<T> outputQueue = outputQueues.get(0);
			for(int i = from; i < to; i ++) {
				if(!outputQueue.offer(buffer.get(i))) {
					return i - from;
				}
			}
			return to - from;
		} else {
			BlockingQueue<T> outputQueue;
			for(int i = from; i < to; i ++) {
				outputQueue = outputQueues.get((int) (rrc.incrementAndGet() % outputsCount));
				if(!outputQueue.offer(buffer.get(i))) {
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
		outputExecutor.shutdownNow();
		for(final BlockingQueue<T> nextOutputQueue : outputQueues) {
			nextOutputQueue.clear();
		}
		outputQueues.clear();
		outputs.clear();
	}
}
