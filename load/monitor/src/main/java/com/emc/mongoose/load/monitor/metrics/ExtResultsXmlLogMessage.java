package com.emc.mongoose.load.monitor.metrics;

import static com.emc.mongoose.common.Constants.M;
import static com.emc.mongoose.common.Constants.MIB;
import static com.emc.mongoose.load.monitor.metrics.IoStats.Snapshot;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.LogMessageBase;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 12.12.16.
 */
public final class ExtResultsXmlLogMessage
extends LogMessageBase {

	private static final DateFormat FMT_DATE_RESULTS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") {
		{
			setTimeZone(TimeZone.getTimeZone("UTC"));
		}
	};

	private final String jobName;
	private final Int2ObjectMap<Snapshot> snapshots;
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;

	public ExtResultsXmlLogMessage(
		final String jobName, final Int2ObjectMap<Snapshot> snapshots,
		final Int2IntMap concurrencyMap, final Int2IntMap driversCountMap
	) {
		this.jobName = jobName;
		this.snapshots = snapshots;
		this.concurrencyMap = concurrencyMap;
		this.driversCountMap = driversCountMap;
	}

	@Override
	public final void formatTo(final StringBuilder buffer) {

		Snapshot snapshot;
		int concurrency;
		int driversCount;

		for(final int ioTypeCode : snapshots.keySet()) {

			snapshot = snapshots.get(ioTypeCode);
			concurrency = concurrencyMap.get(ioTypeCode);
			driversCount = driversCountMap.get(ioTypeCode);

			buffer.append("<result id=\"").append(jobName).append("\" ");
			final long startTimeMillis = TimeUnit.MICROSECONDS.toMillis(snapshot.getStartTime());
			final Date startDate = new Date(startTimeMillis);
			buffer.append("StartDate=\"").append(FMT_DATE_RESULTS.format(startDate)).append("\" ");
			buffer.append("StartTimestamp=\"").append(startTimeMillis).append("\" ");
			final long elapsedTimeMillis = TimeUnit.MICROSECONDS.toMillis(snapshot.getElapsedTime());
			final long endTimeStamp = startTimeMillis + elapsedTimeMillis;
			final Date endDate = new Date(endTimeStamp);
			buffer.append("EndDate=\"").append(FMT_DATE_RESULTS.format(endDate)).append("\" ");
			buffer.append("EndTimestamp=\"").append(endTimeStamp).append("\" ");
			buffer.append("operation=\"").append(IoType.values()[ioTypeCode].name()).append("\" ");
			buffer.append("threads=\"").append(concurrency * driversCount).append("\" ");
			buffer.append("RequestThreads=\"").append(concurrency).append("\" ");
			buffer.append("clients=\"").append(driversCount).append("\" ");
			buffer.append("error=\"").append(snapshot.getFailCount()).append("\" ");
			buffer.append("runtime=\"").append(((float) snapshot.getElapsedTime()) / M).append("\" ");
			final double tp = snapshot.getSuccRateMean();
			final double bw = snapshot.getByteRateMean();
			buffer.append("filesize=\"").append(SizeInBytes.formatFixedSize(tp > 0 ? (long) (bw / tp) : 0)).append("\" ");
			buffer.append("tps=\"").append(tp).append("\" tps_unit=\"Fileps\" ");
			buffer.append("bw=\"").append(bw / MIB).append("\" bw_unit=\"MBps\" ");
			buffer.append("latency=\"").append(snapshot.getLatencyAvg()).append("\" latency_unit=\"us\" ");
			buffer.append("latency_min=\"").append(snapshot.getLatencyMin()).append("\" ");
			buffer.append("latency_loq=\"").append(snapshot.getLatencyLoQ()).append("\" ");
			buffer.append("latency_med=\"").append(snapshot.getLatencyMed()).append("\" ");
			buffer.append("latency_hiq=\"").append(snapshot.getLatencyHiQ()).append("\" ");
			buffer.append("latency_max=\"").append(snapshot.getLatencyMax()).append("\" ");
			buffer.append("duration=\"").append(snapshot.getDurationAvg()).append("\" duration_unit=\"us\" ");
			buffer.append("duration_min=\"").append(snapshot.getDurationMin()).append("\" ");
			buffer.append("duration_loq=\"").append(snapshot.getDurationHiQ()).append("\" ");
			buffer.append("duration_med=\"").append(snapshot.getDurationMed()).append("\" ");
			buffer.append("duration_hiq=\"").append(snapshot.getDurationHiQ()).append("\" ");
			buffer.append("duration_max=\"").append(snapshot.getDurationMax()).append("\" ");
			buffer.append("/>");
		}
	}
}
