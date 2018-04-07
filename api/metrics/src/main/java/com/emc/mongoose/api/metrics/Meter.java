package com.emc.mongoose.api.metrics;

import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
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
		props.put(KEY_STORAGE_DRIVER_COUNT, Integer.toString(metricsCtx.getNodeCount()));
		props.put(KEY_STORAGE_DRIVER_CONCURRENCY, Integer.toString(metricsCtx.getConcurrency()));
		objectName = new ObjectName(METRICS_DOMAIN, props);
		metricsCtx.setMetricsListener(this);
		try {
			MBEAN_SERVER.registerMBean(this, objectName);
		} catch(
			final InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException e
		) {
			LogUtil.exception(Level.WARN, e, "Failed to start the metrics JMX service");
		}
	}

	@Override
	public final void close() {
		metricsCtx.setMetricsListener(null);
		lastSnapshot = null;
		try {
			MBEAN_SERVER.unregisterMBean(objectName);
		} catch(final InstanceNotFoundException | MBeanRegistrationException e) {
			LogUtil.exception(Level.WARN, e, "Failed to unregister the metrics JMX service");
		}
	}

	private volatile MetricsSnapshot lastSnapshot = null;

	@Override
	public final void notify(final MetricsSnapshot snapshot) {
		this.lastSnapshot = snapshot;
	}

	@Override
	public final long getStartTimeMillis() {
		return lastSnapshot.getStartTimeMillis();
	}

	@Override
	public final long getSuccCount() {
		return lastSnapshot.getSuccCount();
	}

	@Override
	public final double getSuccRateMean() {
		return lastSnapshot.getSuccRateMean();
	}

	@Override
	public final double getSuccRateLast() {
		return lastSnapshot.getSuccRateLast();
	}

	@Override
	public final long getFailCount() {
		return lastSnapshot.getFailCount();
	}

	@Override
	public final double getFailRateMean() {
		return lastSnapshot.getFailRateMean();
	}

	@Override
	public final double getFailRateLast() {
		return lastSnapshot.getFailRateLast();
	}

	@Override
	public final long getByteCount() {
		return lastSnapshot.getByteCount();
	}

	@Override
	public final double getByteRateMean() {
		return lastSnapshot.getByteRateMean();
	}

	@Override
	public final double getByteRateLast() {
		return lastSnapshot.getByteRateLast();
	}

	@Override
	public final long getElapsedTimeMillis() {
		return lastSnapshot.getElapsedTimeMillis();
	}

	@Override
	public final int getActualConcurrencyLast() {
		return lastSnapshot.getActualConcurrencyLast();
	}

	@Override
	public final double getActualConcurrencyMean() {
		return lastSnapshot.getActualConcurrencyMean();
	}

	@Override
	public final long getDurationSum() {
		return lastSnapshot.getDurationSum();
	}

	@Override
	public final long getLatencySum() {
		return lastSnapshot.getLatencySum();
	}

	@Override
	public final long getDurationMin() {
		return lastSnapshot.getDurationMin();
	}

	@Override
	public final long getDurationLoQ() {
		return lastSnapshot.getDurationLoQ();
	}

	@Override
	public final long getDurationMed() {
		return lastSnapshot.getDurationMed();
	}

	@Override
	public final long getDurationHiQ() {
		return lastSnapshot.getDurationHiQ();
	}

	@Override
	public final long getDurationMax() {
		return lastSnapshot.getDurationMax();
	}

	@Override
	public final double getDurationMean() {
		return lastSnapshot.getDurationMean();
	}

	@Override
	public long[] getDurationValues() {
		return lastSnapshot.getDurationValues();
	}

	@Override
	public final long getLatencyMin() {
		return lastSnapshot.getLatencyMin();
	}

	@Override
	public final long getLatencyLoQ() {
		return lastSnapshot.getLatencyLoQ();
	}

	@Override
	public final long getLatencyMed() {
		return lastSnapshot.getLatencyMed();
	}

	@Override
	public final long getLatencyHiQ() {
		return lastSnapshot.getLatencyHiQ();
	}

	@Override
	public final long getLatencyMax() {
		return lastSnapshot.getLatencyMax();
	}

	@Override
	public final double getLatencyMean() {
		return lastSnapshot.getLatencyMean();
	}

	@Override
	public long[] getLatencyValues() {
		return lastSnapshot.getLatencyValues();
	}
}
