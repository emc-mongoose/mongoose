package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.model.io.Input;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
/**
 Created by andrey on 06.11.16.
 */
public class PendingIoTaskDispatcher<I extends Item, O extends IoTask<I>>
implements Output<O> {

	private final List<StorageDriver<I, O>> drivers;
	private final int driversCount;
	private final Throttle<Object> rateThrottle;
	private final Throttle<Object> weightThrottle;
	private final Function<O, Object> weightFunc;
	private final AtomicLong rrc = new AtomicLong(0);

	public PendingIoTaskDispatcher(
		final List<StorageDriver<I, O>> drivers, final double rateLimit,
		final Object2IntMap<Object> weightsMap, Function<O, Object> weightFunc
	) {
		this.drivers = drivers;
		this.driversCount = drivers.size();
		this.rateThrottle = new RateThrottle<>(rateLimit);
		this.weightThrottle = new WeightThrottle<>(weightsMap);
		this.weightFunc = weightFunc;
	}

	private StorageDriver<I, O> getNextDriver() {
		if(driversCount > 1) {
			return drivers.get((int) (rrc.incrementAndGet() % driversCount));
		} else {
			return drivers.get(0);
		}
	}

	@Override
	public final void put(final O ioTask)
	throws IOException {
		try {
			if(rateThrottle != null) {
				rateThrottle.waitPassFor(ioTask);
			}
			if(weightThrottle != null) {
				weightThrottle.waitPassFor(weightFunc.apply(ioTask));
			}
			final StorageDriver<I, O> nextDriver = getNextDriver();
			nextDriver.put(ioTask);
		} catch(final InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}

	@Override
	public final int put(final List<O> buffer, final int from, final int to)
	throws IOException {
		int n = to - from;
		if(n > 0) {
			try {
				if(rateThrottle != null) {
					rateThrottle.waitPassFor(buffer, n);
				}
				if(weightThrottle != null) {
					// assuming that all the I/O tasks in the batch share the same weighting criteria
					weightThrottle.waitPassFor(weightFunc.apply(buffer.get(from)), n);
				}
				final StorageDriver<I, O> nextDriver = getNextDriver();
				return nextDriver.put(buffer, from, to);
			} catch(final InterruptedException e) {
				throw new InterruptedIOException(e.getMessage());
			}
		} else {
			return 0;
		}
	}

	@Override
	public final int put(final List<O> buffer)
	throws IOException {
		return put(buffer, 0, buffer.size());
	}

	@Override
	public final Input<O> getInput()
	throws IOException {
		return null;
	}

	@Override
	public final void close()
	throws IOException {
	}
}
