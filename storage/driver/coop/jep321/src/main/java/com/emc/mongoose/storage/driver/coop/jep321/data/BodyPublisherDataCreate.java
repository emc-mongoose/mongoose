package com.emc.mongoose.storage.driver.coop.jep321.data;

import com.emc.mongoose.base.item.op.data.DataOperation;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

/**
 * A Publisher that publishes items obtained from the given Iterable. Each new
 * subscription gets a new Iterator.
 */
public final class BodyPublisherDataCreate
	implements HttpRequest.BodyPublisher {

	// Only one of `dataOp` and `throwable` can be non-null. throwable is
	// non-null when an error has been encountered, by the creator of
	// PullPublisher, while subscribing the subscriber, but before subscribe has
	// completed.
	private final DataOperation dataOp;
	private final Throwable throwable;

	BodyPublisherDataCreate(DataOperation dataOp, Throwable throwable) {
		this.dataOp = dataOp;
		this.throwable = throwable;
	}

	public BodyPublisherDataCreate(DataOperation dataOp) {
		this(dataOp, null);
	}

	@Override
	public final void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
		Subscription sub;
		if (throwable != null) {
			assert dataOp == null : "non-null dataOp: " + dataOp;
			sub = new Subscription(subscriber, null, throwable);
		} else {
			assert throwable == null : "non-null exception: " + throwable;
			sub = new Subscription(subscriber, dataOp, null);
		}
		subscriber.onSubscribe(sub);

		if (throwable != null) {
			sub.pullScheduler.runOrSchedule();
		}
	}

	@Override
	public final long contentLength() {
		try {
			return dataOp.item().size();
		} catch(final IOException e) {
			throw new AssertionError(e);
		}
	}

	private static final class Subscription implements Flow.Subscription {

		private final Flow.Subscriber<? super ByteBuffer> subscriber;
		private final DataOperation dataOp;
		private volatile boolean completed;
		private volatile boolean cancelled;
		private volatile Throwable error;
		final SequentialScheduler pullScheduler = new SequentialScheduler(new PullTask());
		private final Demand demand = new Demand();

		Subscription(Flow.Subscriber<? super ByteBuffer> subscriber,
			DataOperation dataOp,
			Throwable throwable) {
			this.subscriber = subscriber;
			this.dataOp = dataOp;
			this.error = throwable;
		}

		final class PullTask extends SequentialScheduler.CompleteRestartableTask {
			@Override
			protected void run() {
				if (completed || cancelled) {
					return;
				}

				Throwable t = error;
				if (t != null) {
					completed = true;
					pullScheduler.stop();
					subscriber.onError(t);
					return;
				}

				final var iter = dataOp.item();

				while (demand.tryDecrement() && !cancelled) {
					if (!iter.hasNext()) {
						break;
					} else {
						subscriber.onNext(iter.next());
					}
				}
				if (!iter.hasNext() && !cancelled) {
					try {
						dataOp.countBytesDone(iter.size());
					} catch(final IOException e) {
						throw new AssertionError(e);
					}
					completed = true;
					pullScheduler.stop();
					subscriber.onComplete();
				}
			}
		}

		@Override
		public void request(long n) {
			if (cancelled)
				return;  // no-op

			if (n <= 0) {
				error = new IllegalArgumentException("illegal non-positive request:" + n);
			} else {
				demand.increase(n);
			}
			pullScheduler.runOrSchedule();
		}

		@Override
		public void cancel() {
			cancelled = true;
		}
	}
}
