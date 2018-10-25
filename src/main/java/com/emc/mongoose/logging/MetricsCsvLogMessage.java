package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshot;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
import com.emc.mongoose.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshot;
import static com.emc.mongoose.Constants.K;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.env.DateUtil.FMT_DATE_ISO8601;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import java.util.Date;

/**
 Created by kurila on 18.05.17.
 */
@AsynchronouslyFormattable
public final class MetricsCsvLogMessage
extends LogMessageBase {

	private final AllMetricsSnapshot snapshot;
	private final OpType opType;
	private final int concurrencyLimit;

	public MetricsCsvLogMessage(final AllMetricsSnapshot snapshot, final OpType opType, final int concurrencyLimit) {
		this.snapshot = snapshot;
		this.opType = opType;
		this.concurrencyLimit = concurrencyLimit;
	}

	@Override
	public final void formatTo(final StringBuilder strb) {

		final ConcurrencyMetricSnapshot concurrencySnapshot = snapshot.concurrencySnapshot();
		final TimingMetricSnapshot durationSnapshot = snapshot.durationSnapshot();
		final RateMetricSnapshot successCountSnapshot = snapshot.successSnapshot();
		final RateMetricSnapshot byteCountSnapshot = snapshot.byteSnapshot();
		final TimingMetricSnapshot latencySnapshot = snapshot.latencySnapshot();

		strb
			.append('"').append(FMT_DATE_ISO8601.format(new Date())).append('"').append(',')
			.append(opType.name()).append(',')
			.append(concurrencyLimit).append(',')
			.append(
				snapshot instanceof DistributedAllMetricsSnapshot ? ((DistributedAllMetricsSnapshot) snapshot).nodeCount() : 1
			)
			.append(',')
			.append(concurrencySnapshot.last()).append(',')
			.append(concurrencySnapshot.mean()).append(',')
			.append(successCountSnapshot.count()).append(',')
			.append(snapshot.failsSnapshot().count()).append(',')
			.append(byteCountSnapshot.count()).append(',')
			.append(snapshot.elapsedTimeMillis() / K).append(',')
			.append(durationSnapshot.sum() / M).append(',')
			.append(successCountSnapshot.mean()).append(',')
			.append(successCountSnapshot.last()).append(',')
			.append(byteCountSnapshot.mean()).append(',')
			.append(byteCountSnapshot.last()).append(',')
			.append(durationSnapshot.mean()).append(',')
			.append(durationSnapshot.min()).append(',')
			.append(durationSnapshot.histogramSnapshot().quantile(0.25)).append(',')
			.append(durationSnapshot.histogramSnapshot().quantile(0.5)).append(',')
			.append(durationSnapshot.histogramSnapshot().quantile(0.75)).append(',')
			.append(durationSnapshot.max()).append(',')
			.append(latencySnapshot.mean()).append(',')
			.append(latencySnapshot.min()).append(',')
			.append(latencySnapshot.histogramSnapshot().quantile(0.25)).append(',')
			.append(latencySnapshot.histogramSnapshot().quantile(0.5)).append(',')
			.append(latencySnapshot.histogramSnapshot().quantile(0.75)).append(',')
			.append(latencySnapshot.max());
	}
}
