package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.common.Constants;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.MessageBase;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.commons.lang.text.StrBuilder;

import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 26.10.16.
 */
public final class MetricsTableLogMessage
extends MessageBase {
	
	private static final String TABLE_HEADER_LINES[] = new String[] {
		"______________________________________________________________________________________________________________",
		" Load | Total |       Count       |  Job  |    TP [op/s]    |        |  BW [MB/s]  |Latency [us]|Duration [us]",
		" Type | Concur|-------------------| Time  |-----------------|  Size  |-------------|------------|-------------",
		"      | rency |   Success  |Failed|  [s]  |  Mean  |  Last  |        | Mean | Last |    Mean    |    Mean     ",
		"------|-------|------------|------|-------|--------|--------|--------|------|------|------------|-------------"
	};
	
	private final String runId;
	private final Int2ObjectMap<IoStats.Snapshot> snapshots;
	private final int totalConcurrency;
	
	public MetricsTableLogMessage(
		final String runId, final Int2ObjectMap<IoStats.Snapshot> snapshots,
		final int totalConcurrency
	) {
		this.runId = runId;
		this.snapshots = snapshots;
		this.totalConcurrency = totalConcurrency;
	}

	@Override
	public final void formatTo(final StringBuilder buffer) {
		final StrBuilder strb = new StrBuilder(runId).append(" metrics:");
		if(snapshots.size() > 0) {
			strb.appendNewLine();
			for(final String tableHeaderLine : TABLE_HEADER_LINES) {
				strb.append(tableHeaderLine).appendNewLine();
			}
			IoStats.Snapshot snapshot;
			for(final int ioTypeCode : snapshots.keySet()) {
				snapshot = snapshots.get(ioTypeCode);
				strb.appendFixedWidthPadLeft(IoType.values()[ioTypeCode].name(), 6, ' ').append('|');
				strb.appendFixedWidthPadLeft(totalConcurrency, 7, ' ').append('|');
				strb.appendFixedWidthPadLeft(snapshot.getSuccCount(), 12, ' ').append(('|'));
				strb.appendFixedWidthPadLeft(snapshot.getFailCount(), 6, ' ').append('|');
				strb
					.appendFixedWidthPadLeft(
						TimeUnit.MICROSECONDS.toSeconds(snapshot.getElapsedTime()), 7, ' '
					)
					.append('|');
				strb.appendFixedWidthPadRight(snapshot.getSuccRateMean(), 8, ' ').append('|');
				strb.appendFixedWidthPadRight(snapshot.getSuccRateLast(), 8, ' ').append('|');
				strb
					.appendFixedWidthPadLeft(
						SizeInBytes.formatFixedSize(snapshot.getByteCount()), 8, ' '
					)
					.append('|');
				strb
					.appendFixedWidthPadRight(snapshot.getByteRateMean() / Constants.MIB, 6, ' ')
					.append('|');
				strb
					.appendFixedWidthPadRight(snapshot.getByteRateLast() / Constants.MIB, 6, ' ')
					.append('|');
				strb.appendFixedWidthPadLeft((int) snapshot.getLatencyAvg(), 12, ' ').append('|');
				strb.appendFixedWidthPadLeft((int) snapshot.getDurationAvg(), 12, ' ');
				strb.appendNewLine();
			}
			strb.appendPadding(110, '-');
		} else {
			strb.append(" not available yet");
		}
		buffer.append(strb.toString());
	}
}
