package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.codahale.metrics.Gauge;
//
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.util.pool.Reusable;
//
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 17.12.14.
 */
public final class GaugeValueTask<V extends Number>
implements Callable<V>, Reusable {
	//
	private volatile Gauge<V> gauge = null;
	//
	@Override
	public final V call()
		throws Exception {
		return gauge.getValue();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<GaugeValueTask>
		POOL = new InstancePool<>(GaugeValueTask.class);
	//
	public static GaugeValueTask<? extends Number> getInstance(final Gauge<? extends Number> gauge) {
		return (GaugeValueTask<? extends Number>) POOL.take(gauge);
	}
	//
	private final AtomicBoolean isClosed = new AtomicBoolean(true);
	//
	@Override @SuppressWarnings("unchecked")
	public final GaugeValueTask<V> reuse(final Object... args)
	throws IllegalArgumentException {
		if(isClosed.compareAndSet(true, false)) {
			if(args==null) {
				throw new IllegalArgumentException("No arguments for reusing the instance");
			}
			if(args.length > 0) {
				gauge = (Gauge<V>) args[0];
			}
		} else {
			throw new IllegalStateException("Not yet released instance reuse attempt");
		}
		return this;
	}
	//
	@Override
	public final void close() {
		if(isClosed.compareAndSet(false, true)) {
			POOL.release(this);
		}
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
}
