package com.emc.mongoose.client.impl.load.executor.tasks;
//
import com.codahale.metrics.Gauge;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
/**
 Created by kurila on 17.12.14.
 */
public final class GaugeValuePeriodicTask<V extends Number>
implements PeriodicTask<V> {
	//
	private final Gauge<V> gauge;
	private volatile V result = null;
	//
	public GaugeValuePeriodicTask(final Gauge<V> gauge) {
		this.gauge = gauge;
	}
	//
	@Override
	public final void run() {
		result = gauge.getValue();
	}
	//
	@Override
	public final V getLastResult() {
		return result == null ? gauge.getValue() : result;
	}
}
