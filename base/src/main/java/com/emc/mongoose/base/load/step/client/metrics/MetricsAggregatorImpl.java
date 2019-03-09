package com.emc.mongoose.base.load.step.client.metrics;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import com.emc.mongoose.base.load.step.LoadStep;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;

public final class MetricsAggregatorImpl extends AsyncRunnableBase implements MetricsAggregator {

	private final String loadStepId;
	private final MetricsSnapshotsSupplierTask[] snapshotSuppliers;
	private final int count;

	public MetricsAggregatorImpl(final String loadStepId, final List<LoadStep> stepSlices) {
		this.loadStepId = loadStepId;
		snapshotSuppliers = stepSlices
						.stream()
						.map(MetricsSnapshotsSupplierTaskImpl::new)
						.collect(Collectors.toList())
						.toArray(new MetricsSnapshotsSupplierTask[]{});
		count = snapshotSuppliers.length;
	}

	public final List<AllMetricsSnapshot> metricsSnapshotsByIndex(final int originIndex) {
		MetricsSnapshotsSupplierTask supplyTask;
		List<? extends AllMetricsSnapshot> snapshots;
		AllMetricsSnapshot snapshot;
		final List<AllMetricsSnapshot> snapshotsByIndex = new ArrayList<>(count);
		for (var i = 0; i < count; i++) {
			supplyTask = snapshotSuppliers[i];
			snapshots = supplyTask.get();
			if (originIndex < snapshots.size()) {
				snapshot = snapshots.get(originIndex);
				if (null != snapshot) {
					snapshotsByIndex.add(snapshot);
				}
			}
		}
		return snapshotsByIndex;
	}

	@Override
	protected final void doStart() {
		Arrays.stream(snapshotSuppliers)
						.forEach(
										snapshotsSupplier -> {
											try (final var logCtx = put(KEY_STEP_ID, loadStepId).put(KEY_CLASS_NAME, getClass().getSimpleName())) {
												snapshotsSupplier.start();
											} catch (final RemoteException e) {
												LogUtil.exception(
																Level.ERROR,
																e,
																"{}: failed to start the metrics snapshots supplier task",
																loadStepId);
											}
										});
	}

	@Override
	protected final void doStop() {
		Arrays.stream(snapshotSuppliers)
						.parallel()
						.forEach(
										snapshotsSupplier -> {
											try (final var logCtx = put(KEY_STEP_ID, loadStepId).put(KEY_CLASS_NAME, getClass().getSimpleName())) {
												snapshotsSupplier.stop();
											} catch (final IOException e) {
												LogUtil.exception(
																Level.WARN, e, "{}: failed to stop the metrics snapshot supplier", loadStepId);
											}
										});
	}

	@Override
	protected final void doClose() {
		for (var i = 0; i < count; i++) {
			try (final var logCtx = put(KEY_STEP_ID, loadStepId).put(KEY_CLASS_NAME, getClass().getSimpleName())) {
				snapshotSuppliers[i].close();
			} catch (final IOException e) {
				LogUtil.exception(
								Level.WARN, e, "{}: failed to close the metrics snapshot supplier", loadStepId);
			}
			snapshotSuppliers[i] = null;
		}
	}
}
