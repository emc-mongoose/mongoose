package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.codahale.metrics.Gauge;
//
import com.emc.mongoose.util.threading.PeriodicTask;
/**
 Created by kurila on 17.12.14.
 */
public final class GaugeValuePeriodicTask<V extends Number>
implements PeriodicTask<V> {
	//
	private final Gauge<V> gauge;
	private V result = null;
	//
	public GaugeValuePeriodicTask(final Gauge<V> gauge) {
		this.gauge = gauge;
	}
	//
	@Override
	public final synchronized void run() {
		result = gauge.getValue();
	}
	//
	@Override
	public final synchronized V getLastResult() {
		return result;
	}
}
