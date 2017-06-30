package com.emc.mongoose.load.monitor;

import static com.emc.mongoose.common.Constants.MIB;
import static com.emc.mongoose.model.metrics.MetricsContext.Snapshot;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.log.LogMessageBase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

/**
 Created by andrey on 12.12.16.
 */
@AsynchronouslyFormattable
public final class ExtResultsXmlLogMessage
extends LogMessageBase {

	private static final DateFormat FMT_DATE_RESULTS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") {
		{
			setTimeZone(TimeZone.getTimeZone("UTC"));
		}
	};

	private final MetricsContext metricsCtx;

	public ExtResultsXmlLogMessage(final MetricsContext metricsCtx) {
		this.metricsCtx = metricsCtx;
	}

	@Override
	public final void formatTo(final StringBuilder buffer) {
		buffer.append("<result id=\"").append(metricsCtx.getStepId()).append("\" ");
		final Snapshot snapshot = metricsCtx.getLastSnapshot();
		final long startTimeMillis = snapshot.getStartTime();
		final Date startDate = new Date(startTimeMillis);
		buffer.append("StartDate=\"").append(FMT_DATE_RESULTS.format(startDate)).append("\" ");
		buffer.append("StartTimestamp=\"").append(startTimeMillis).append("\" ");
		final long elapsedTimeMillis = snapshot.getElapsedTime();
		final long endTimeStamp = startTimeMillis + elapsedTimeMillis;
		final Date endDate = new Date(endTimeStamp);
		buffer.append("EndDate=\"").append(FMT_DATE_RESULTS.format(endDate)).append("\" ");
		buffer.append("EndTimestamp=\"").append(endTimeStamp).append("\" ");
		final int ioTypeCode = metricsCtx.getIoType().ordinal();
		buffer.append("operation=\"").append(IoType.values()[ioTypeCode].name()).append("\" ");
		final int concurrency = metricsCtx.getConcurrency();
		final int driversCount = metricsCtx.getDriverCount();
		buffer.append("threads=\"").append(concurrency * driversCount).append("\" ");
		buffer.append("RequestThreads=\"").append(concurrency).append("\" ");
		buffer.append("clients=\"").append(driversCount).append("\" ");
		buffer.append("error=\"").append(snapshot.getFailCount()).append("\" ");
		buffer.append("runtime=\"").append(((float) elapsedTimeMillis) / 1000).append("\" ");
		final String transferSize = SizeInBytes.formatFixedSize(metricsCtx.getTransferSizeEstimate());
		buffer.append("filesize=\"").append(transferSize).append("\" ");
		buffer.append("tps=\"").append(snapshot.getSuccRateMean()).append("\" tps_unit=\"Fileps\" ");
		buffer.append("bw=\"").append(snapshot.getByteRateMean() / MIB).append("\" bw_unit=\"MBps\" ");
		buffer.append("latency=\"").append(snapshot.getLatencyMean()).append("\" latency_unit=\"us\" ");
		buffer.append("latency_min=\"").append(snapshot.getLatencyMin()).append("\" ");
		buffer.append("latency_loq=\"").append(snapshot.getLatencyLoQ()).append("\" ");
		buffer.append("latency_med=\"").append(snapshot.getLatencyMed()).append("\" ");
		buffer.append("latency_hiq=\"").append(snapshot.getLatencyHiQ()).append("\" ");
		buffer.append("latency_max=\"").append(snapshot.getLatencyMax()).append("\" ");
		buffer.append("duration=\"").append(snapshot.getDurationMean()).append("\" duration_unit=\"us\" ");
		buffer.append("duration_min=\"").append(snapshot.getDurationMin()).append("\" ");
		buffer.append("duration_loq=\"").append(snapshot.getDurationLoQ()).append("\" ");
		buffer.append("duration_med=\"").append(snapshot.getDurationMed()).append("\" ");
		buffer.append("duration_hiq=\"").append(snapshot.getDurationHiQ()).append("\" ");
		buffer.append("duration_max=\"").append(snapshot.getDurationMax()).append("\" ");
		buffer.append("/>\n");
	}
}
