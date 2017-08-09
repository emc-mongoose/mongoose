package com.emc.mongoose.api.metrics;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Hashtable;

import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.model.svc.ServiceUtil.MBEAN_SERVER;

/**
 Created by andrey on 05.07.17.
 */
public final class Meter
implements MeterMBean {

	private final MetricsContext metricsCtx;
	private final ObjectName objectName;

	public Meter(final MetricsContext metricsCtx)
	throws MalformedObjectNameException {
		this.metricsCtx = metricsCtx;
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(KEY_TEST_STEP_ID, metricsCtx.getStepId());
		props.put(KEY_LOAD_TYPE, metricsCtx.getIoType().name());
		props.put(KEY_STORAGE_DRIVER_COUNT, Integer.toString(metricsCtx.getDriverCount()));
		props.put(KEY_STORAGE_DRIVER_CONCURRENCY, Integer.toString(metricsCtx.getConcurrency()));
		objectName = new ObjectName(METRICS_DOMAIN, props);
		metricsCtx.setMetricsListener(this);
		try {
			MBEAN_SERVER.registerMBean(this, objectName);
		} catch(final InstanceAlreadyExistsException e) {

		} catch(final MBeanRegistrationException e) {

		} catch(final NotCompliantMBeanException e) {

		}
	}

	@Override
	public final void close()
	throws IOException {
		metricsCtx.setMetricsListener(null);
		try {
			MBEAN_SERVER.unregisterMBean(objectName);
		} catch(final InstanceNotFoundException e) {

		} catch(final MBeanRegistrationException e) {

		}
	}

	private long startTimeMillis;
	private long succCount;
	private double succRateMean;
	private double succRateLast;
	private long failCount;
	private double failRateMean;
	private double failRateLast;
	private long byteCount;
	private double byteRateMean;
	private double byteRateLast;
	private long elapsedTimeMillis;
	private long durationSum;
	private long latencySum;
	private long durationMin;
	private long durationLoQ;
	private long durationMed;
	private long durationHiQ;
	private long durationMax;
	private long latencyMin;
	private long latencyLoQ;
	private long latencyMed;
	private long latencyHiQ;
	private long latencyMax;
	private double durationMean;
	private double latencyMean;

	@Override
	public final void notify(final MetricsContext.Snapshot snapshot) {
		this.startTimeMillis = snapshot.getStartTimeMillis();
		this.succCount = snapshot.getSuccCount();
		this.succRateMean = snapshot.getSuccRateMean();
		this.succRateLast = snapshot.getSuccRateLast();
		this.failCount = snapshot.getFailCount();
		this.failRateMean = snapshot.getFailRateMean();
		this.failRateLast = snapshot.getFailRateLast();
		this.byteCount = snapshot.getByteCount();
		this.byteRateMean = snapshot.getByteRateMean();
		this.byteRateLast = snapshot.getByteRateLast();
		this.elapsedTimeMillis = snapshot.getElapsedTimeMillis();
		this.durationSum = snapshot.getDurationSum();
		this.latencySum = snapshot.getLatencySum();
		this.durationMin = snapshot.getDurationMin();
		this.durationLoQ = snapshot.getDurationLoQ();
		this.durationMed = snapshot.getDurationMed();
		this.durationHiQ = snapshot.getDurationHiQ();
		this.durationMax = snapshot.getDurationMax();
		this.latencyMin = snapshot.getLatencyMin();
		this.latencyLoQ = snapshot.getLatencyLoQ();
		this.latencyMed = snapshot.getLatencyMed();
		this.latencyHiQ = snapshot.getLatencyHiQ();
		this.latencyMax = snapshot.getLatencyMax();
		this.durationMean = snapshot.getDurationMean();
		this.latencyMean = snapshot.getLatencyMean();
	}

	@Override
	public final long getStartTimeMillis() {
		return startTimeMillis;
	}

	@Override
	public final long getSuccCount() {
		return succCount;
	}

	@Override
	public final double getSuccRateMean() {
		return succRateMean;
	}

	@Override
	public final double getSuccRateLast() {
		return succRateLast;
	}

	@Override
	public final long getFailCount() {
		return failCount;
	}

	@Override
	public final double getFailRateMean() {
		return failRateMean;
	}

	@Override
	public final double getFailRateLast() {
		return failRateLast;
	}

	@Override
	public final long getByteCount() {
		return byteCount;
	}

	@Override
	public final double getByteRateMean() {
		return byteRateMean;
	}

	@Override
	public final double getByteRateLast() {
		return byteRateLast;
	}

	@Override
	public final long getElapsedTimeMillis() {
		return elapsedTimeMillis;
	}

	@Override
	public final long getDurationSum() {
		return durationSum;
	}

	@Override
	public final long getLatencySum() {
		return latencySum;
	}

	@Override
	public final long getDurationMin() {
		return durationMin;
	}

	@Override
	public final long getDurationLoQ() {
		return durationLoQ;
	}

	@Override
	public final long getDurationMed() {
		return durationMed;
	}

	@Override
	public final long getDurationHiQ() {
		return durationHiQ;
	}

	@Override
	public final long getDurationMax() {
		return durationMax;
	}

	@Override
	public final double getDurationMean() {
		return durationMean;
	}

	@Override
	public final long getLatencyMin() {
		return latencyMin;
	}

	@Override
	public final long getLatencyLoQ() {
		return latencyLoQ;
	}

	@Override
	public final long getLatencyMed() {
		return latencyMed;
	}

	@Override
	public final long getLatencyHiQ() {
		return latencyHiQ;
	}

	@Override
	public final long getLatencyMax() {
		return latencyMax;
	}

	@Override
	public final double getLatencyMean() {
		return latencyMean;
	}
}
