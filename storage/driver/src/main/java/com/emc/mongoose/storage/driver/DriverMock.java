package com.emc.mongoose.storage.driver;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Monitor;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 11.07.16.
 This mock just passes the submitted tasks to the load monitor em
 */
public class DriverMock<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Driver<I, O> {

	private final Monitor<I, O> monitor;

	public DriverMock(final Monitor<I, O> monitor) {
		this.monitor = monitor;
	}

	@Override
	public boolean isFullThrottleEntered() {
		return true;
	}

	@Override
	public boolean isFullThrottleExited() {
		return false;
	}

	private final static class SubmitFutureMock<I extends Item, O extends IoTask<I>>
	implements Future<O> {

		private final O ioTask;

		public SubmitFutureMock(final O ioTask) {
			this.ioTask = ioTask;
		}

		@Override
		public final boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public final boolean isCancelled() {
			return false;
		}

		@Override
		public final boolean isDone() {
			return true;
		}

		@Override
		public final O get() {
			return ioTask;
		}

		@Override
		public final O get(final long timeout, final TimeUnit unit) {
			return ioTask;
		}
	}
	//
	@Override
	public Future<O> submit(final O task)
	throws RejectedExecutionException {
		monitor.ioTaskCompleted(task);
		return new SubmitFutureMock<>(task);
	}

	@Override
	public int submit(final List<O> tasks, final int from, final int to)
	throws RejectedExecutionException {
		monitor.ioTaskCompletedBatch(tasks, from, to);
		return to - from;
	}

	@Override
	public void start()
	throws IllegalStateException {
	}

	@Override
	public void shutdown()
	throws IllegalStateException {
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return false;
	}

	@Override
	public boolean await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}

	@Override
	public void interrupt() {
	}
}
