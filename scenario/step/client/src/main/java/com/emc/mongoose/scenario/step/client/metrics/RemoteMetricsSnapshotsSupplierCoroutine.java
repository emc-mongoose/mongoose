package com.emc.mongoose.scenario.step.client.metrics;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.scenario.step.LoadStepService;
import com.emc.mongoose.ui.log.LogUtil;
import com.github.akurilov.concurrent.coroutine.CoroutinesExecutor;
import com.github.akurilov.concurrent.coroutine.ExclusiveCoroutineBase;
import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.util.List;

public final class RemoteMetricsSnapshotsSupplierCoroutine
extends ExclusiveCoroutineBase
implements MetricsSnapshotsSupplierCoroutine {

	private final LoadStepService loadStepSvc;
	private volatile List<MetricsSnapshot> snapshotsByOrigin;

	public RemoteMetricsSnapshotsSupplierCoroutine(
		final CoroutinesExecutor executor, final LoadStepService loadStepSvc
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
