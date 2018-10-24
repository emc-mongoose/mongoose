package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshot;
import com.emc.mongoose.metrics.snapshot.HistogramSnapshot;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
import com.emc.mongoose.metrics.context.MetricsContext;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshot;
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
		final AllMetricsSnapshot snapshot = metricsCtx.lastSnapshot();
		final long startTimeMillis = metricsCtx.startTimeStamp();
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
		final int concurrency = metricsCtx.concurrencyLimit();
		final int nodeCount =
			(snapshot instanceof DistributedAllMetricsSnapshot) ? ((DistributedAllMetricsSnapshot) snapshot).nodeCount() : 1;
		buffer.append("threads=\"").append(concurrency * nodeCount).append("\" ");
		buffer.append("RequestThreads=\"").append(concurrency).append("\" ");
		buffer.append("clients=\"").append(nodeCount).append("\" ");
		buffer.append("error=\"").append(snapshot.failsSnapshot().count()).append("\" ");
		buffer.append("runtime=\"").append(((float) elapsedTimeMillis) / 1000).append("\" ");
		final String itemDataSizeStr = metricsCtx.itemDataSize().toString();
		buffer.append("filesize=\"").append(itemDataSizeStr).append("\" ");
		buffer.append("tps=\"").append(snapshot.successSnapshot().mean()).append("\" tps_unit=\"Fileps\" ");
		buffer.append("bw=\"").append(snapshot.byteSnapshot().mean() / MIB).append("\" bw_unit=\"MBps\" ");
		final TimingMetricSnapshot latencySnapshot = snapshot.latencySnapshot();
		buffer.append("latency=\"").append(latencySnapshot.mean()).append("\" latency_unit=\"us\" ");
		buffer.append("latency_min=\"").append(latencySnapshot.min()).append("\" ");
		final HistogramSnapshot latencyHistogramSnapshot = latencySnapshot.histogramSnapshot();
		buffer.append("latency_loq=\"").append(latencyHistogramSnapshot.quantile(0.25)).append("\" ");
		buffer.append("latency_med=\"").append(latencyHistogramSnapshot.quantile(0.5)).append("\" ");
		buffer.append("latency_hiq=\"").append(latencyHistogramSnapshot.quantile(0.75)).append("\" ");
		buffer.append("latency_max=\"").append(latencySnapshot.max()).append("\" ");
		final TimingMetricSnapshot durationSnapshot = snapshot.durationSnapshot();
		buffer.append("duration=\"").append(durationSnapshot.mean()).append("\" duration_unit=\"us\" ");
		buffer.append("duration_min=\"").append(durationSnapshot.min()).append("\" ");
		final HistogramSnapshot durationHistogramSnapshot = durationSnapshot.histogramSnapshot();
		buffer.append("duration_loq=\"").append(durationHistogramSnapshot.quantile(0.25)).append("\" ");
		buffer.append("duration_med=\"").append(durationHistogramSnapshot.quantile(0.5)).append("\" ");
		buffer.append("duration_hiq=\"").append(durationHistogramSnapshot.quantile(0.75)).append("\" ");
		buffer.append("duration_max=\"").append(durationSnapshot.max()).append("\" ");
		buffer.append("/>\n");
	}
}
