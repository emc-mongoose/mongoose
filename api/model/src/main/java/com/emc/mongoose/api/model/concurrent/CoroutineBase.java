package com.emc.mongoose.api.model.concurrent;

//import com.codahale.metrics.Counter;
//import com.codahale.metrics.Histogram;
//import com.codahale.metrics.JmxReporter;
//import com.codahale.metrics.MetricRegistry;

import com.emc.mongoose.api.common.concurrent.StoppableTaskBase;
//import com.emc.mongoose.api.model.svc.ServiceUtil;

import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 26.07.17.
 The base class for all coroutines.
 */
public abstract class CoroutineBase
extends StoppableTaskBase
implements Coroutine {

	//private final static MetricRegistry METRIC_REGISTRY = new MetricRegistry();
	//private final static JmxReporter METRIC_REPORTER = JmxReporter
	//	.forRegistry(METRIC_REGISTRY)
	//	.inDomain(Coroutine.class.getPackage().getName())
	//	.registerWith(ServiceUtil.MBEAN_SERVER)
	//	.build();
	//static {
	//	METRIC_REPORTER.start();
	//}

	private final List<Coroutine> coroutineRegistry;
	//private final Histogram durations;
	//private final Counter durationsSum;

	protected CoroutineBase(final List<Coroutine> coroutineRegistry) {
		this.coroutineRegistry = coroutineRegistry;
		//this.durations = METRIC_REGISTRY.histogram(getClass().getSimpleName() + "-durations");
		//this.durationsSum = METRIC_REGISTRY.counter(getClass().getSimpleName() + "-durationsSum");
	}

	/**
	 Decorates the invocation method with timing.
	 */
	@Override
	protected final void invoke() {
		long t = System.nanoTime();
		invokeTimed(t);
		//t = System.nanoTime() - t;
		//if(t > TIMEOUT_NANOS) {
		//	System.err.println(
		//		"Coroutine \"" + toString() + "\" invocation duration exceeded the limit: " + t +
		//			"[ns]"
		//	);
		//}
		//durations.update(t);
		//durationsSum.inc();
	}

	/**
	 The method implementation should use the start time to check its own duration in order to not
	 to exceed the invocation time limit (250ms)
	 @param startTimeNanos the time when the invocation started
	 */
	protected abstract void invokeTimed(final long startTimeNanos);

	@Override
	public final void close()
	throws IOException {
		coroutineRegistry.remove(this);
		super.close();
	}
}
