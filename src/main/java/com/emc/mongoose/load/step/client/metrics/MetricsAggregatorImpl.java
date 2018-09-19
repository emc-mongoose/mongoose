package com.emc.mongoose.load.step.client.metrics;

import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.logging.LogUtil;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import com.emc.mongoose.metrics.MetricsSnapshot;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class MetricsAggregatorImpl
extends AsyncRunnableBase
implements MetricsAggregator {

	private final String loadStepId;
	private final Map<LoadStep, MetricsSnapshotsSupplierTask> snapshotSuppliers;

	public MetricsAggregatorImpl(final String loadStepId, final List<LoadStep> stepSlices) {
		this.loadStepId = loadStepId;
		snapshotSuppliers = stepSlices
			.stream()
			.collect(Collectors.toMap(Function.identity(), MetricsSnapshotsSupplierTaskImpl::new));
	}

	public final List<MetricsSnapshot> metricsSnapshotsByIndex(final int originIndex) {
		return snapshotSuppliers
			.values()
			.stream()
			.map(Supplier::get)
			.filter(Objects::nonNull)
			.map(
				metricsSnapshots -> originIndex < metricsSnapshots.size() ? metricsSnapshots.get(originIndex) : null
			)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Override
	protected final void doStart() {
		snapshotSuppliers
			.values()
			.forEach(
				snapshotsSupplier -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, loadStepId)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						snapshotsSupplier.start();
					} catch(final RemoteException e) {
						LogUtil.exception(
							Level.ERROR, e, "{}: failed to start the metrics snapshots supplier task", loadStepId
						);
					}
				}
			);
	}

	@Override
	protected final void doStop() {
		snapshotSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsSupplier -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, loadStepId)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						snapshotsSupplier.stop();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to stop the metrics snapshot supplier", loadStepId
						);
					}
				}
			);
	}

	@Override
	protected final void doClose() {
		snapshotSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsSupplier -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, loadStepId)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						snapshotsSupplier.close();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to close the metrics snapshot supplier", loadStepId
						);
					}
				}
			);
		snapshotSuppliers.clear();
	}
}
