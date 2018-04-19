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
		props.put(KEY_TEST_STEP_ID, metricsCtx.stepId());
		props.put(KEY_LOAD_TYPE, metricsCtx.ioType().name());
		props.put(KEY_STORAGE_DRIVER_COUNT, Integer.toString(metricsCtx.nodeCount()));
		props.put(KEY_STORAGE_DRIVER_CONCURRENCY, Integer.toString(metricsCtx.concurrency()));
		objectName = new ObjectName(METRICS_DOMAIN, props);
		metricsCtx.metricsListener(this);
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
		metricsCtx.metricsListener(null);
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
