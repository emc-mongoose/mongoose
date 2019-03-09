package com.emc.mongoose.base.concurrent.disruptor;

import com.emc.mongoose.base.logging.LogContextThreadFactory;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.io.Input;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class BatchIssuingOutputImpl<T>
				extends AsyncRunnableBase
				implements BatchIssuingOutput<T> {

	private static final ThreadFactory TF = new LogContextThreadFactory(
					BatchIssuingOutputImpl.class.getSimpleName(), true);
	private static final WaitStrategy WAIT_STRATEGY = new SleepingWaitStrategy();

	private final Disruptor<EventWithPayload<T>> disruptor;
	private final RingBuffer<EventWithPayload<T>> ringBuff;

	public BatchIssuingOutputImpl(final int ringBuffSize) {
		final var evtFactory = new EventWithPayloadFactory<T>();
		disruptor = new Disruptor<>(evtFactory, ringBuffSize, TF, ProducerType.MULTI, WAIT_STRATEGY);
		ringBuff = disruptor.getRingBuffer();
	}

	@Override
	protected final void doStart() {
		disruptor.start();
	}

	@Override
	protected final void doStop() {
		disruptor.halt();
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit) {
		try {
			disruptor.shutdown(timeout, timeUnit);
		} catch (final TimeoutException e) {
			return false;
		}
		return true;
	}

	@Override
	public final boolean put(final T item) {
		try {
			final var seq = ringBuff.tryNext();
			final var evt = ringBuff.get(seq);
			evt.payload(item);
			ringBuff.publish(seq);
			return true;
		} catch (final InsufficientCapacityException e) {
			return false;
		}
	}

	@Override
	public final int put(final List<T> buffer, final int from, final int to) {
		// find the count of the items to put
		var srcCount = to - from;
		// check
		if (srcCount == 0) {
			return 0;
		}
		// check for the degenerate batch mode
		if (srcCount == 1) {
			if (put(buffer.get(from))) {
				return srcCount;
			} else {
				return 0;
			}
		}
		// acquire some permits
		long end = -1;
		while (srcCount > 0) {
			try {
				end = ringBuff.tryNext(srcCount);
				// some permits have been acquired
				break;
			} catch (final InsufficientCapacityException e) {
				// failed to acquire this count of permits, try to acquire the twice lower count of permits
				srcCount /= 2;
			}
		}
		// continue only if some permits have been acquired
		if (-1 != end) {
			final var beg = end - (srcCount - 1);
			EventWithPayload<T> evt;
			for (var j = beg; j <= end; j++) {
				evt = ringBuff.get(j);
				evt.payload(buffer.get(from + (int) (j - beg)));
			}
			ringBuff.publish(beg, end);
		}
		return srcCount;
	}

	@Override
	public final int put(final List<T> buffer) {
		return put(buffer, 0, buffer.size());
	}

	@Override
	public final Input<T> getInput() {
		throw new AssertionError("Not implemented");
	}

	@Override
	public final int remaining() {
		return ringBuff.getBufferSize() - (int) ringBuff.remainingCapacity();
	}

	@Override
	public final void register(final Consumer<List<T>> batchHandler) {
		disruptor.handleEventsWith(new BatchEventWithPayloadHandler<>(batchHandler));
	}
}
