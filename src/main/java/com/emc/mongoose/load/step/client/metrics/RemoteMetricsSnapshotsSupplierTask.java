package com.emc.mongoose.load.step.client.metrics;

import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.load.step.LoadStepService;
import com.emc.mongoose.logging.LogUtil;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;
import com.github.akurilov.fiber4j.FibersExecutor;

import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.util.List;

public final class RemoteMetricsSnapshotsSupplierTask
extends ExclusiveFiberBase
implements MetricsSnapshotsSupplierTask {

	private final LoadStepService loadStepSvc;
	private volatile List<MetricsSnapshot> snapshotsByOrigin;

	public RemoteMetricsSnapshotsSupplierTask(
		final FibersExecutor executor, final LoadStepService loadStepSvc
	) {
		super(executor);
		this.loadStepSvc = loadStepSvc;
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		try {
			snapshotsByOrigin = loadStepSvc.metricsSnapshots();
		} catch(final RemoteException e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to fetch the metrics snapshots from \"{}\"", loadStepSvc
			);
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
