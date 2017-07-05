package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.log.LogMessageBase;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import static com.emc.mongoose.common.Constants.K;
import static com.emc.mongoose.common.Constants.M;
import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_ISO8601;

import java.util.Date;

/**
 Created by kurila on 18.05.17.
 
 TypeLoad,
 Concurrency,
 DriverCount,
 CountSucc,
 CountFail,
 Size,
 JobDuration[s],
 DurationSum[s],
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
		final MetricsContext.Snapshot snapshot = metricsCtx.getLastSnapshot();
		strb
			.append('"').append(FMT_DATE_ISO8601.format(new Date())).append('"').append(',')
			.append(metricsCtx.getIoType().name()).append(',')
			.append(metricsCtx.getConcurrency()).append(',')
			.append(metricsCtx.getDriverCount()).append(',')
			.append(snapshot.getSuccCount()).append(',')
			.append(snapshot.getFailCount()).append(',')
			.append(snapshot.getByteCount()).append(',')
			.append(snapshot.getElapsedTimeMillis() / K).append(',')
			.append(snapshot.getDurationSum() / M).append(',')
			.append(snapshot.getSuccRateMean()).append(',')
			.append(snapshot.getSuccRateLast()).append(',')
			.append(snapshot.getByteRateMean()).append(',')
			.append(snapshot.getByteRateLast()).append(',')
			.append(snapshot.getDurationMean()).append(',')
			.append(snapshot.getDurationMin()).append(',')
			.append(snapshot.getDurationLoQ()).append(',')
			.append(snapshot.getDurationMed()).append(',')
			.append(snapshot.getDurationHiQ()).append(',')
			.append(snapshot.getDurationMax()).append(',')
			.append(snapshot.getDurationMean()).append(',')
			.append(snapshot.getLatencyMin()).append(',')
			.append(snapshot.getLatencyLoQ()).append(',')
			.append(snapshot.getLatencyMed()).append(',')
			.append(snapshot.getLatencyHiQ()).append(',')
			.append(snapshot.getLatencyMax());
	}
}
