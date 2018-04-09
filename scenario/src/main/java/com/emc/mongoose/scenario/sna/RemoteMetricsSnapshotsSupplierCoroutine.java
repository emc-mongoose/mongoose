package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.ui.log.LogUtil;

import com.github.akurilov.concurrent.coroutines.CoroutinesExecutor;
import com.github.akurilov.concurrent.coroutines.ExclusiveCoroutineBase;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;

public final class RemoteMetricsSnapshotsSupplierCoroutine
extends ExclusiveCoroutineBase
implements MetricsSnapshotsSupplierCoroutine {

	private final LoadStepService loadStepSvc;
	private volatile Int2ObjectMap<MetricsSnapshot> snapshotsByOrigin;

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
	public final Int2ObjectMap<MetricsSnapshot> get() {
		return snapshotsByOrigin;
	}
}
