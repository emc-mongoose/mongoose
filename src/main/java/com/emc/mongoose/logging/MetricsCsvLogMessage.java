package com.emc.mongoose.logging;

import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsSnapshot;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import static com.emc.mongoose.Constants.K;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.env.DateUtil.FMT_DATE_ISO8601;

import java.util.Date;

/**
 Created by kurila on 18.05.17.
 
 OpLoad,
 Concurrency,
 DriverCount,
 ConcurrencyCurr,
 ConcurrencyMean,
 CountSucc,
 CountFail,
 Size,
 JobDuration[s],
 DurationSum[s],
 ActualConcurrency,
 TPAvg[op/s],
 TPLast[op/s],
 BWAvg[MB/s],
 BWLast[MB/s],
 DurationAvg[us]
 DurationMin[us],
 DurationLoQ[us],
 DurationMed[us],
 DurationHiQ[us],
 DurationMax[us],
 LatencyAvg[us]
 LatencyMin[us],
 LatencyLoQ[us],
 LatencyMed[us],
 LatencyHiQ[us],
 LatencyMax[us]
 */
@AsynchronouslyFormattable
public final class MetricsCsvLogMessage
extends LogMessageBase {
	
	private final MetricsContext metricsCtx;
	
	public MetricsCsvLogMessage(final MetricsContext metricsCtx) {
		this.metricsCtx = metricsCtx;
	}
	
	@Override
	public final void formatTo(final StringBuilder strb) {
		final MetricsSnapshot snapshot = metricsCtx.lastSnapshot();
		strb
			.append('"').append(FMT_DATE_ISO8601.format(new Date())).append('"').append(',')
			.append(metricsCtx.ioType().name()).append(',')
			.append(metricsCtx.concurrencyLimit()).append(',')
			.append(metricsCtx.nodeCount()).append(',')
			.append(snapshot.actualConcurrencyLast()).append(',')
			.append(snapshot.actualConcurrencyMean()).append(',')
			.append(snapshot.succCount()).append(',')
			.append(snapshot.failCount()).append(',')
			.append(snapshot.byteCount()).append(',')
			.append(snapshot.elapsedTimeMillis() / K).append(',')
			.append(snapshot.durationSum() / M).append(',')
			.append(snapshot.succRateMean()).append(',')
			.append(snapshot.succRateLast()).append(',')
			.append(snapshot.byteRateMean()).append(',')
			.append(snapshot.byteRateLast()).append(',')
			.append(snapshot.durationMean()).append(',')
			.append(snapshot.durationMin()).append(',')
			.append(snapshot.durationLoQ()).append(',')
			.append(snapshot.durationMed()).append(',')
			.append(snapshot.durationHiQ()).append(',')
			.append(snapshot.durationMax()).append(',')
			.append(snapshot.latencyMean()).append(',')
			.append(snapshot.latencyMin()).append(',')
			.append(snapshot.latencyLoQ()).append(',')
			.append(snapshot.latencyMed()).append(',')
			.append(snapshot.latencyHiQ()).append(',')
			.append(snapshot.latencyMax());
	}
}
