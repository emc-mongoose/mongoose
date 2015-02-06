package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.codahale.metrics.Gauge;
//
import java.util.concurrent.Callable;
/**
 Created by kurila on 17.12.14.
 */
public final class GaugeValueTask<V extends Number>
implements Callable<V> {
	//
	private volatile Gauge<V> gauge = null;
	//
	public GaugeValueTask(final Gauge<V> gauge) {
		this.gauge = gauge;
	}
	//
	@Override
	public final V call()
	throws Exception {
		return gauge.getValue();
	}
}
