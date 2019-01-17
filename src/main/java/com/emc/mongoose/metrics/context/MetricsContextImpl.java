package com.emc.mongoose.metrics.context;

import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_BYTE;
import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_CONC;
import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_DUR;
import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_FAIL;
import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_LAT;
import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_SUCC;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshotImpl;
import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshot;
import com.emc.mongoose.metrics.type.ConcurrencyMeterImpl;
import com.emc.mongoose.metrics.type.HistogramImpl;
import com.emc.mongoose.metrics.type.LongMeter;
import com.emc.mongoose.metrics.type.RateMeter;
import com.emc.mongoose.metrics.type.RateMeterImpl;
import com.emc.mongoose.metrics.type.TimingMeterImpl;
import com.emc.mongoose.metrics.util.ConcurrentSlidingWindowLongReservoir;
import com.github.akurilov.commons.system.SizeInBytes;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntSupplier;

public class MetricsContextImpl<S extends AllMetricsSnapshotImpl> extends MetricsContextBase<S>
    implements MetricsContext<S> {

  private final LongMeter<TimingMetricSnapshot> reqDuration, respLatency;
  private final LongMeter<ConcurrencyMetricSnapshot> actualConcurrency;
  private final RateMeter<RateMetricSnapshot> throughputSuccess, throughputFail, reqBytes;
  private volatile TimingMetricSnapshot reqDurSnapshot, respLatSnapshot;
  private volatile ConcurrencyMetricSnapshot actualConcurrencySnapshot;
  private final IntSupplier actualConcurrencyGauge;
  private final ReadWriteLock timingLock = new ReentrantReadWriteLock();
  private final Lock timingLockUpdate = timingLock.readLock();
  private final Lock timingLockRefresh = timingLock.writeLock();

  public MetricsContextImpl(
      final String id,
      final OpType opType,
      final IntSupplier actualConcurrencyGauge,
      final int concurrencyLimit,
      final int concurrencyThreshold,
      final SizeInBytes itemDataSize,
      final int updateIntervalSec,
      final boolean stdOutColorFlag,
      final String comment) {
    super(
        id,
        opType,
        concurrencyLimit,
        1,
        concurrencyThreshold,
        itemDataSize,
        stdOutColorFlag,
        TimeUnit.SECONDS.toMillis(updateIntervalSec),
        comment);
    //
    respLatency =
        new TimingMeterImpl(
            new HistogramImpl(new ConcurrentSlidingWindowLongReservoir(DEFAULT_RESERVOIR_SIZE)),
            METRIC_NAME_LAT);
    respLatSnapshot = respLatency.snapshot();
    //
    reqDuration =
        new TimingMeterImpl(
            new HistogramImpl(new ConcurrentSlidingWindowLongReservoir(DEFAULT_RESERVOIR_SIZE)),
            METRIC_NAME_DUR);
    reqDurSnapshot = reqDuration.snapshot();
    //
    this.actualConcurrencyGauge = actualConcurrencyGauge;
    actualConcurrency = new ConcurrencyMeterImpl(METRIC_NAME_CONC);
    actualConcurrencySnapshot = actualConcurrency.snapshot();
    //
    final Clock clock = Clock.systemUTC();
    //
    throughputSuccess = new RateMeterImpl(clock, METRIC_NAME_SUCC);
    //
    throughputFail = new RateMeterImpl(clock, METRIC_NAME_FAIL);
    //
    reqBytes = new RateMeterImpl(clock, METRIC_NAME_BYTE);
  }

  @Override
  public final void start() {
    super.start();
    throughputSuccess.resetStartTime();
    throughputFail.resetStartTime();
    reqBytes.resetStartTime();
  }

  @Override
  public final void markSucc(final long bytes, final long duration, final long latency) {
    throughputSuccess.update(1);
    reqBytes.update(bytes);
    updateTimings(latency, duration);
    if (thresholdMetricsCtx != null) {
      thresholdMetricsCtx.markSucc(bytes, duration, latency);
    }
  }

  @Override
  public final void markPartSucc(final long bytes, final long duration, final long latency) {
    reqBytes.update(bytes);
    updateTimings(latency, duration);
    if (thresholdMetricsCtx != null) {
      thresholdMetricsCtx.markPartSucc(bytes, duration, latency);
    }
  }

  @Override
  public final void markSucc(
      final long count, final long bytes, final long durationValues[], final long latencyValues[]) {
    throughputSuccess.update(count);
    reqBytes.update(bytes);
    final int timingsLen = Math.min(durationValues.length, latencyValues.length);
    long duration, latency;
    for (int i = 0; i < timingsLen; ++i) {
      duration = durationValues[i];
      latency = latencyValues[i];
      updateTimings(latency, duration);
    }
    if (thresholdMetricsCtx != null) {
      thresholdMetricsCtx.markSucc(count, bytes, durationValues, latencyValues);
    }
  }

  @Override
  public final void markPartSucc(
      final long bytes, final long durationValues[], final long latencyValues[]) {
    reqBytes.update(bytes);
    final int timingsLen = Math.min(durationValues.length, latencyValues.length);
    long duration, latency;
    for (int i = 0; i < timingsLen; ++i) {
      duration = durationValues[i];
      latency = latencyValues[i];
      updateTimings(latency, duration);
    }
    if (thresholdMetricsCtx != null) {
      thresholdMetricsCtx.markPartSucc(bytes, durationValues, latencyValues);
    }
  }

  private void updateTimings(final long latencyMicros, final long durationMicros) {
    if (latencyMicros > 0 && durationMicros > latencyMicros) {
      timingLockUpdate.lock();
      try {
        reqDuration.update(durationMicros);
        respLatency.update(latencyMicros);
      } finally {
        timingLockUpdate.unlock();
      }
    }
  }

  @Override
  public final void markFail() {
    throughputFail.update(1);
    if (thresholdMetricsCtx != null) {
      thresholdMetricsCtx.markFail();
    }
  }

  @Override
  public final void markFail(final long count) {
    throughputFail.update(count);
    if (thresholdMetricsCtx != null) {
      thresholdMetricsCtx.markFail(count);
    }
  }

  @Override
  public final boolean avgPersistEnabled() {
    return false;
  }

  @Override
  public final boolean sumPersistEnabled() {
    return false;
  }

  @Override
  public final boolean perfDbResultsFileEnabled() {
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void refreshLastSnapshot() {
    final long currentTimeMillis = System.currentTimeMillis();
    if (currentTimeMillis - lastOutputTs() > DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS) {
      refreshTimings();
      actualConcurrency.update(actualConcurrencyGauge.getAsInt());
      actualConcurrencySnapshot = actualConcurrency.snapshot();
    }
    lastSnapshot =
        (S)
            new AllMetricsSnapshotImpl(
                reqDurSnapshot,
                respLatSnapshot,
                actualConcurrencySnapshot,
                throughputFail.snapshot(),
                throughputSuccess.snapshot(),
                reqBytes.snapshot(),
                elapsedTimeMillis());
    super.refreshLastSnapshot();
  }

  private void refreshTimings() {
    if (timingLockRefresh.tryLock()) {
      try {
        reqDurSnapshot = reqDuration.snapshot();
        respLatSnapshot = respLatency.snapshot();
      } finally {
        timingLockRefresh.unlock();
      }
    }
  }

  @Override
  protected MetricsContextImpl<S> newThresholdMetricsContext() {
    return new ContextBuilderImpl()
        .id(id())
        .opType(opType)
        .actualConcurrencyGauge(actualConcurrencyGauge)
        .concurrencyLimit(concurrencyLimit)
        .concurrencyThreshold(0)
        .itemDataSize(itemDataSize)
        .outputPeriodSec((int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis))
        .stdOutColorFlag(stdOutColorFlag)
        .build();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(final Object other) {
    if (null == other) {
      return false;
    }
    if (other instanceof MetricsContextImpl) {
      return 0 == compareTo((MetricsContextImpl<S>) other);
    } else {
      return false;
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "("
        + opType.name()
        + '-'
        + concurrencyLimit
        + "x1@"
        + id
        + ")";
  }

  @Override
  public final void close() {
    super.close();
  }

  public static ContextBuilder builder() {
    return new ContextBuilderImpl();
  }

  private static class ContextBuilderImpl
      implements ContextBuilder<ContextBuilder, MetricsContextImpl> {

    private IntSupplier actualConcurrencyGauge;
    private String id;
    private OpType opType;
    private int concurrencyLimit;
    private int concurrencyThreshold;
    private SizeInBytes itemDataSize;
    private boolean stdOutColorFlag;
    private int outputPeriodSec;
    private String comment;

    public MetricsContextImpl build() {
      return new MetricsContextImpl(
          id,
          opType,
          actualConcurrencyGauge,
          concurrencyLimit,
          concurrencyThreshold,
          itemDataSize,
          outputPeriodSec,
          stdOutColorFlag,
          comment);
    }

    public ContextBuilderImpl id(final String id) {
      this.id = id;
      return this;
    }

    @Override
    public ContextBuilder comment(final String comment) {
      this.comment = comment;
      return this;
    }

    public ContextBuilderImpl opType(final OpType opType) {
      this.opType = opType;
      return this;
    }

    public ContextBuilderImpl concurrencyLimit(final int concurrencyLimit) {
      this.concurrencyLimit = concurrencyLimit;
      return this;
    }

    public ContextBuilderImpl concurrencyThreshold(final int concurrencyThreshold) {
      this.concurrencyThreshold = concurrencyThreshold;
      return this;
    }

    public ContextBuilderImpl itemDataSize(final SizeInBytes itemDataSize) {
      this.itemDataSize = itemDataSize;
      return this;
    }

    public ContextBuilderImpl stdOutColorFlag(final boolean stdOutColorFlag) {
      this.stdOutColorFlag = stdOutColorFlag;
      return this;
    }

    public ContextBuilderImpl outputPeriodSec(final int outputPeriodSec) {
      this.outputPeriodSec = outputPeriodSec;
      return this;
    }

    public ContextBuilderImpl actualConcurrencyGauge(final IntSupplier actualConcurrencyGauge) {
      this.actualConcurrencyGauge = actualConcurrencyGauge;
      return this;
    }
  }
}
