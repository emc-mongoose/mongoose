package com.emc.mongoose.client.impl.load.executor.tasks;
//
import com.codahale.metrics.CachedGauge;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
/**
 Created by kurila on 17.12.14.
 */
public final class CachedGaugePeriodicTask<V extends Number>
implements PeriodicTask<V> {
	//
	private final CachedGauge<V> gauge;
	//
	public CachedGaugePeriodicTask(final CachedGauge<V> gauge) {
		this.gauge = gauge;
	}
	//
	@Override
	public final void run() {
		gauge.getValue();
	}
	//
	@Override
	public final V getLastResult() {
		return gauge.getValue();
	}
}
