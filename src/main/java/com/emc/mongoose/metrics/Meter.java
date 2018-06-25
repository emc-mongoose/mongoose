package com.emc.mongoose.metrics;

import javax.management.ObjectName;
import java.util.Hashtable;

import static com.emc.mongoose.Constants.KEY_STEP_ID;
import static com.emc.mongoose.svc.ServiceUtil.MBEAN_SERVER;

/**
 Created by andrey on 05.07.17.
 */
public final class Meter
implements MeterMBean {

	private final MetricsContext metricsCtx;
	private final ObjectName objectName;

	public Meter(final MetricsContext metricsCtx)
	throws Exception {
		this.metricsCtx = metricsCtx;
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(KEY_STEP_ID, metricsCtx.stepId());
		props.put(KEY_LOAD_TYPE, metricsCtx.ioType().name());
		props.put(KEY_NODE_COUNT, Integer.toString(metricsCtx.nodeCount()));
		props.put(KEY_CONCURRENCY_LIMIT, Integer.toString(metricsCtx.concurrencyLimit()));
		objectName = new ObjectName(METRICS_DOMAIN, props);
		metricsCtx.metricsListener(this);
		MBEAN_SERVER.registerMBean(this, objectName);
	}

	@Override
	public final void close()
	throws Exception {
		metricsCtx.metricsListener(null);
		lastSnapshot = null;
		MBEAN_SERVER.unregisterMBean(objectName);
	}

	private volatile MetricsSnapshot lastSnapshot = null;

	@Override
	public final void notify(final MetricsSnapshot snapshot) {
		this.lastSnapshot = snapshot;
	}

	@Override
	public final long startTimeMillis() {
		return lastSnapshot.startTimeMillis();
	}

	@Override
	public final long succCount() {
		return lastSnapshot.succCount();
	}

	@Override
	public final double succRateMean() {
		return lastSnapshot.succRateMean();
	}

	@Override
	public final double succRateLast() {
		return lastSnapshot.succRateLast();
	}

	@Override
	public final long failCount() {
		return lastSnapshot.failCount();
	}

	@Override
	public final double failRateMean() {
		return lastSnapshot.failRateMean();
	}

	@Override
	public final double failRateLast() {
		return lastSnapshot.failRateLast();
	}

	@Override
	public final long byteCount() {
		return lastSnapshot.byteCount();
	}

	@Override
	public final double byteRateMean() {
		return lastSnapshot.byteRateMean();
	}

	@Override
	public final double byteRateLast() {
		return lastSnapshot.byteRateLast();
	}

	@Override
	public final long elapsedTimeMillis() {
		return lastSnapshot.elapsedTimeMillis();
	}

	@Override
	public final int actualConcurrencyLast() {
		return lastSnapshot.actualConcurrencyLast();
	}

	@Override
	public final double actualConcurrencyMean() {
		return lastSnapshot.actualConcurrencyMean();
	}

	@Override
	public final long durationSum() {
		return lastSnapshot.durationSum();
	}

	@Override
	public final long latencySum() {
		return lastSnapshot.latencySum();
	}

	@Override
	public final long durationMin() {
		return lastSnapshot.durationMin();
	}

	@Override
	public final long durationLoQ() {
		return lastSnapshot.durationLoQ();
	}

	@Override
	public final long durationMed() {
		return lastSnapshot.durationMed();
	}

	@Override
	public final long durationHiQ() {
		return lastSnapshot.durationHiQ();
	}

	@Override
	public final long durationMax() {
		return lastSnapshot.durationMax();
	}

	@Override
	public final double durationMean() {
		return lastSnapshot.durationMean();
	}

	@Override
	public long[] durationValues() {
		return lastSnapshot.durationValues();
	}

	@Override
	public final long latencyMin() {
		return lastSnapshot.latencyMin();
	}

	@Override
	public final long latencyLoQ() {
		return lastSnapshot.latencyLoQ();
	}

	@Override
	public final long latencyMed() {
		return lastSnapshot.latencyMed();
	}

	@Override
	public final long latencyHiQ() {
		return lastSnapshot.latencyHiQ();
	}

	@Override
	public final long latencyMax() {
		return lastSnapshot.latencyMax();
	}

	@Override
	public final double latencyMean() {
		return lastSnapshot.latencyMean();
	}

	@Override
	public long[] latencyValues() {
		return lastSnapshot.latencyValues();
	}
}
