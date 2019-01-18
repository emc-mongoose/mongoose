package com.emc.mongoose.load.step.client.metrics;

import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.fiber4j.ExclusiveFiberBase;
import com.github.akurilov.fiber4j.FibersExecutor;
import java.util.List;
import org.apache.logging.log4j.Level;

public final class MetricsSnapshotsSupplierTaskImpl extends ExclusiveFiberBase
    implements MetricsSnapshotsSupplierTask {

  private final LoadStep loadStep;
  private volatile List<? extends AllMetricsSnapshot> snapshotsByOrigin;

  public MetricsSnapshotsSupplierTaskImpl(final LoadStep loadStep) {
    this(ServiceTaskExecutor.INSTANCE, loadStep);
  }

  public MetricsSnapshotsSupplierTaskImpl(final FibersExecutor executor, final LoadStep loadStep) {
    super(executor);
    this.loadStep = loadStep;
  }

  @Override
  protected final void invokeTimedExclusively(final long startTimeNanos) {
    try {
      snapshotsByOrigin = loadStep.metricsSnapshots();
    } catch (final Exception e) {
      LogUtil.exception(
          Level.DEBUG, e, "Failed to fetch the metrics snapshots from \"{}\"", loadStep);
    }
  }

  @Override
  public final List<? extends AllMetricsSnapshot> get() {
    return snapshotsByOrigin;
  }

  @Override
  protected final void doClose() {
    if (null != snapshotsByOrigin) {
      snapshotsByOrigin.clear();
    }
  }
}
