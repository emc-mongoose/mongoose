package com.emc.mongoose.load.step.metrics;

import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.logging.LogUtil;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;
import com.github.akurilov.fiber4j.FibersExecutor;

import org.apache.logging.log4j.Level;

import java.util.List;

public final class MetricsSnapshotsSupplierTaskImpl
extends ExclusiveFiberBase
implements MetricsSnapshotsSupplierTask {

	private final LoadStep loadStep;
	private volatile List<MetricsSnapshot> snapshotsByOrigin;

	public MetricsSnapshotsSupplierTaskImpl(final FibersExecutor executor, final LoadStep loadStep) {
		super(executor);
		this.loadStep = loadStep;
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		try {
			snapshotsByOrigin = loadStep.metricsSnapshots();
		} catch(final Exception e) {
			LogUtil.exception(Level.WARN, e, "Failed to fetch the metrics snapshots from \"{}\"", loadStep);
		}
	}

	@Override
	public final List<MetricsSnapshot> get() {
		return snapshotsByOrigin;
	}

	@Override
	protected final void doClose() {
		snapshotsByOrigin.clear();
	}
}
