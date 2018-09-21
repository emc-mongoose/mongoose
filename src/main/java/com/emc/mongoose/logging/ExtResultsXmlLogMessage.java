package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsSnapshot;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.emc.mongoose.Constants.MIB;

/**
 Created by andrey on 12.12.16.
 */
@AsynchronouslyFormattable public final class ExtResultsXmlLogMessage
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
		buffer.append("<result id=\"").append(metricsCtx.id()).append("\" ");
		final MetricsSnapshot snapshot = metricsCtx.lastSnapshot();
		final long startTimeMillis = snapshot.startTimeMillis();
		final Date startDate = new Date(startTimeMillis);
		buffer.append("StartDate=\"").append(FMT_DATE_RESULTS.format(startDate)).append("\" ");
		buffer.append("StartTimestamp=\"").append(startTimeMillis).append("\" ");
		final long elapsedTimeMillis = snapshot.elapsedTimeMillis();
		final long endTimeStamp = startTimeMillis + elapsedTimeMillis;
		final Date endDate = new Date(endTimeStamp);
		buffer.append("EndDate=\"").append(FMT_DATE_RESULTS.format(endDate)).append("\" ");
		buffer.append("EndTimestamp=\"").append(endTimeStamp).append("\" ");
		final int ioTypeCode = metricsCtx.opType().ordinal();
		buffer.append("operation=\"").append(OpType.values()[ioTypeCode].name()).append("\" ");
		final int concurrency = snapshot.concurrencyLimit();
		final int nodeCount =
			(snapshot instanceof DistributedMetricsSnapshot) ? ((DistributedMetricsSnapshot) snapshot).nodeCount() : 1;
		buffer.append("threads=\"").append(concurrency * nodeCount).append("\" ");
		buffer.append("RequestThreads=\"").append(concurrency).append("\" ");
		buffer.append("clients=\"").append(nodeCount).append("\" ");
		buffer.append("error=\"").append(snapshot.failCount()).append("\" ");
		buffer.append("runtime=\"").append(((float) elapsedTimeMillis) / 1000).append("\" ");
		final String itemDataSizeStr = metricsCtx.itemDataSize().toString();
		buffer.append("filesize=\"").append(itemDataSizeStr).append("\" ");
		buffer.append("tps=\"").append(snapshot.succRateMean()).append("\" tps_unit=\"Fileps\" ");
		buffer.append("bw=\"").append(snapshot.byteRateMean() / MIB).append("\" bw_unit=\"MBps\" ");
		buffer.append("latency=\"").append(snapshot.latencyMean()).append("\" latency_unit=\"us\" ");
		buffer.append("latency_min=\"").append(snapshot.latencyMin()).append("\" ");
		buffer.append("latency_loq=\"").append(snapshot.latencyLoQ()).append("\" ");
		buffer.append("latency_med=\"").append(snapshot.latencyMed()).append("\" ");
		buffer.append("latency_hiq=\"").append(snapshot.latencyHiQ()).append("\" ");
		buffer.append("latency_max=\"").append(snapshot.latencyMax()).append("\" ");
		buffer.append("duration=\"").append(snapshot.durationMean()).append("\" duration_unit=\"us\" ");
		buffer.append("duration_min=\"").append(snapshot.durationMin()).append("\" ");
		buffer.append("duration_loq=\"").append(snapshot.durationLoQ()).append("\" ");
		buffer.append("duration_med=\"").append(snapshot.durationMed()).append("\" ");
		buffer.append("duration_hiq=\"").append(snapshot.durationHiQ()).append("\" ");
		buffer.append("duration_max=\"").append(snapshot.durationMax()).append("\" ");
		buffer.append("/>\n");
	}
}
