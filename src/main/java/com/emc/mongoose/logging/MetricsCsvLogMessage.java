package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.metrics.context.MetricsContext;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import java.util.Date;

import static com.emc.mongoose.Constants.K;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.env.DateUtil.FMT_DATE_ISO8601;

/**
 Created by kurila on 18.05.17.
 */
@AsynchronouslyFormattable
public final class MetricsCsvLogMessage
	extends LogMessageBase {

	private final MetricsSnapshot snapshot;
	private final OpType opType;
	private final int concurrencyLimit;

	public MetricsCsvLogMessage(final MetricsContext metricsCtx) {
		this.opType = metricsCtx.opType();
		this.snapshot = metricsCtx.lastSnapshot();
		this.concurrencyLimit = metricsCtx.concurrencyLimit();
	}

	@Override
	public final void formatTo(final StringBuilder strb) {
		strb
			.append('"').append(FMT_DATE_ISO8601.format(new Date())).append('"').append(',')
			.append(opType.name()).append(',')
			.append(concurrencyLimit).append(',')
			.append(
				snapshot instanceof DistributedMetricsSnapshot ? ((DistributedMetricsSnapshot) snapshot).nodeCount() : 1
			)
			.append(',')
			.append(snapshot.concurrencySnapshot().mean()).append(',')
			.append(snapshot.concurrencySnapshot().mean()).append(',')
			.append(snapshot.successSnapshot().count()).append(',')
			.append(snapshot.failsSnapshot().count()).append(',')
			.append(snapshot.byteSnapshot().count()).append(',')
			.append(snapshot.elapsedTimeMillis() / K).append(',')
			.append(snapshot.durationSnapshot().sum() / M).append(',')
			.append(snapshot.successSnapshot().mean()).append(',')
			.append(snapshot.successSnapshot().last()).append(',')
			.append(snapshot.byteSnapshot().mean()).append(',')
			.append(snapshot.byteSnapshot().last()).append(',')
			.append(snapshot.durationSnapshot().mean()).append(',')
			.append(snapshot.durationSnapshot().min()).append(',')
			.append(snapshot.durationSnapshot().quantile(0.25)).append(',')
			.append(snapshot.durationSnapshot().quantile(0.5)).append(',')
			.append(snapshot.durationSnapshot().quantile(0.75)).append(',')
			.append(snapshot.durationSnapshot().max()).append(',')
			.append(snapshot.latencySnapshot().mean()).append(',')
			.append(snapshot.latencySnapshot().min()).append(',')
			.append(snapshot.latencySnapshot().quantile(0.25)).append(',')
			.append(snapshot.latencySnapshot().quantile(0.5)).append(',')
			.append(snapshot.latencySnapshot().quantile(0.75)).append(',')
			.append(snapshot.latencySnapshot().max());
	}
}
