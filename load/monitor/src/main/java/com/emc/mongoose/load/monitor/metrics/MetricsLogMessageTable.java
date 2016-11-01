package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.common.Constants;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.ui.log.MessageBase;

import org.apache.commons.lang.text.StrBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 26.10.16.
 */
public final class MetricsLogMessageTable
extends MessageBase {
	
	private final static String TABLE_HEADER_LINES[] = new String[] {
		"=====================================================================================================================",
		" Load  |          Count        |   Job   |    TP [op/s]    |         |    BW [MB/s]    | Latency [us] | Duration [us]",
		" Type  |-----------------------|  Time   |-----------------|  Size   |-----------------|--------------|--------------",
		"       |   Success    | Failed |   [s]   |  Mean  |  Last  |         |  Mean  |  Last  |     Mean     |     Mean     ",
		"-------|--------------|--------|---------|--------|--------|---------|--------|--------|--------------|--------------"
	};
	
	private final String runId;
	private final Map<LoadType, IoStats.Snapshot> snapshots;
	
	public MetricsLogMessageTable(
		final String runId, final Map<LoadType, IoStats.Snapshot> snapshots
	) {
		this.runId = runId;
		this.snapshots = snapshots;
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
			for(final LoadType loadType : snapshots.keySet()) {
				snapshot = snapshots.get(loadType);
				strb.appendFixedWidthPadRight(loadType.name(), 7, ' ').append("| ");
				strb.appendFixedWidthPadRight(snapshot.getSuccCount(), 13, ' ').append("| ");
				strb.appendFixedWidthPadRight(snapshot.getFailCount(), 7, ' ').append("| ");
				strb
					.appendFixedWidthPadRight(
						TimeUnit.MICROSECONDS.toSeconds(snapshot.getElapsedTime()), 8, ' '
					)
					.append("| ");
				strb.appendFixedWidthPadRight(snapshot.getSuccRateMean(), 7, ' ').append("| ");
				strb.appendFixedWidthPadRight(snapshot.getSuccRateLast(), 7, ' ').append("| ");
				strb
					.appendFixedWidthPadRight(
						SizeInBytes.formatFixedSize(snapshot.getByteCount()), 8, ' '
					)
					.append("| ");
				strb
					.appendFixedWidthPadRight(snapshot.getByteRateMean() / Constants.MIB, 7, ' ')
					.append("| ");
				strb
					.appendFixedWidthPadRight(snapshot.getByteRateLast() / Constants.MIB, 7, ' ')
					.append("| ");
				strb.appendFixedWidthPadRight((int) snapshot.getLatencyAvg(), 13, ' ').append("| ");
				strb.appendFixedWidthPadRight((int) snapshot.getDurationAvg(), 13, ' ');
				strb.appendNewLine();
			}
			strb.appendPadding(117, '=');
		} else {
			strb.append(" not available yet");
		}
		buffer.append(strb.toString());
	}
}
