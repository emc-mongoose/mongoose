package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.ui.log.LogMessageBase;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.text.MessageFormat;
/**
 Created by andrey on 12.12.16.
 */
public final class ExtResultsXmlLogMessage
extends LogMessageBase {

	private static final MessageFormat REC_FMT = new MessageFormat(
		"<result\n" + "id=\\\"%X{run.id}\\\"\n" + "StartDate=\\\"%X{start.date}\\\"\n" +
		"StartTimestamp=\\\"%X{start.time}\\\"\n" + "EndDate=\\\"%X{end.date}\\\"\n" +
		"EndTimestamp=\\\"%X{end.time}\\\"\n" + "threads=\\\"%X{total.threads}\\\"\n" +
		"filesize=\\\"%X{item.data.size}\\\"\n" + "bw=\\\"$14\\\"\n" +
		"bw_unit=\\\"MBps\\\"\n" + "clients=\\\"$6\\\"\n" + "error=\\\"$8\\\"\n" +
		"latency=\\\"$22\\\"\n" + "latency_unit=\\\"us\\\"\n" + "operation=\\\"$3\\\"\n" +
		"runtime=\\\"$10\\\"\n" + "RequestThreads=\\\"$4\\\"\n" + "tps=\\\"$12\\\"\n" +
		"tps_unit=\\\"Fileps\\\"\n" + "duration=\\\"$16\\\"\n" + "duration_unit=\\\"us\\\"\n" +
		"latency_min=\\\"$23\\\"\n" + "latency_loq=\\\"$24\\\"\n" +
		"latency_med=\\\"$25\\\"\n" + "latency_hiq=\\\"$26\\\"\n" +
		"latency_max=\\\"$27\\\"\n" + "duration_min=\\\"$17\\\"\n" +
		"duration_loq=\\\"$18\\\"\n" + "duration_med=\\\"$19\\\"\n" +
		"duration_hiq=\\\"$20\\\" \n" + "duration_max=\n" + "/>"
	);

	private final String jobName;
	private final Int2ObjectMap<IoStats.Snapshot> snapshots;
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;

	public ExtResultsXmlLogMessage(
		final String jobName, final Int2ObjectMap<IoStats.Snapshot> snapshots,
		final Int2IntMap concurrencyMap, final Int2IntMap driversCountMap
	) {
		this.jobName = jobName;
		this.snapshots = snapshots;
		this.concurrencyMap = concurrencyMap;
		this.driversCountMap = driversCountMap;
	}

	@Override
	public final void formatTo(final StringBuilder buffer) {
	}
}
