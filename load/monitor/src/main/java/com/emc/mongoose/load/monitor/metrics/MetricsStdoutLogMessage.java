package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.LogMessageBase;
import static com.emc.mongoose.common.Constants.M;
import static com.emc.mongoose.common.Constants.MIB;
import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;

import com.emc.mongoose.ui.log.LogUtil;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.commons.lang.text.StrBuilder;

/**
 Created by kurila on 26.10.16.
 */
public final class MetricsStdoutLogMessage
extends LogMessageBase {
	
	private static final String TABLE_HEADER_LINES[] = new String[] {
		"______________________________________________________________________________________________________________________",
		" Load | Concur| Driver|       Count       |  Job  |    TP [op/s]    |        |  BW [MB/s]  |Latency [us]|Duration [us]",
		" Type | rency | Count |-------------------| Time  |-----------------|  Size  |-------------|------------|-------------",
		"      |       |       |   Success  |Failed|  [s]  |  Mean  |  Last  |        | Mean | Last |    Mean    |    Mean     ",
		"------|-------|-------|------------|------|-------|--------|--------|--------|------|------|------------|-------------"
	};
	
	private final String jobName;
	private final Int2ObjectMap<IoStats.Snapshot> snapshots;
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;
	
	public MetricsStdoutLogMessage(
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
		if(snapshots.size() == 1) {
			final int ioTypeCode = snapshots.keySet().iterator().nextInt();
			formatSingleSnapshot(
				buffer, jobName, ioTypeCode, snapshots.get(ioTypeCode),
				concurrencyMap.get(ioTypeCode), driversCountMap.values().iterator().nextInt()
			);
		} else {
			formatMultiSnapshot(buffer);
		}
	}

	private static String getColoredFailCountText(final long succCount, final long failCount) {
		final String failCountText;
		if(LogUtil.isConsoleColoringEnabled()) {
			if(failCount > 0) {
				if(succCount / failCount > 100) {
					failCountText = LogUtil.YELLOW + Long.toString(failCount) + LogUtil.GREEN;
				} else {
					failCountText = LogUtil.RED + Long.toString(failCount) + LogUtil.GREEN;
				}
			} else {
				failCountText = Long.toString(failCount);
			}
		} else {
			failCountText = Long.toString(failCount);
		}
		return failCountText;
	}

	private static void formatSingleSnapshot(
		final StringBuilder buffer, final String runId, final int ioTypeCode,
		final IoStats.Snapshot snapshot, final int concurrency, final int driversCount
	) {
		final long succCount = snapshot.getSuccCount();
		buffer
			.append("\n\t")
			.append(IoType.values()[ioTypeCode]).append('-')
			.append(concurrency).append('x').append(driversCount)
			.append(": n=(").append(succCount).append('/')
			.append(getColoredFailCountText(succCount, snapshot.getFailCount())).append("); t[s]=(")
			.append(formatFixedWidth(snapshot.getElapsedTime() / 1000.0, 7)).append('/')
			.append(formatFixedWidth(snapshot.getDurationSum() / M, 7)).append("); size=(")
			.append(formatFixedSize(snapshot.getByteCount())).append("); TP[op/s]=(")
			.append(formatFixedWidth(snapshot.getSuccRateMean(), 7)).append('/')
			.append(formatFixedWidth(snapshot.getSuccRateLast(), 7)).append("); BW[MB/s]=(")
			.append(formatFixedWidth(snapshot.getByteRateMean() / MIB, 6)).append('/')
			.append(formatFixedWidth(snapshot.getByteRateLast() / MIB, 6)).append("); dur[us]=(")
			.append((long) snapshot.getDurationAvg()).append('/')
			.append(snapshot.getDurationMin()).append('/')
			.append(snapshot.getDurationMax()).append("); lat[us]=(")
			.append((long) snapshot.getLatencyAvg()).append('/')
			.append(snapshot.getLatencyMin()).append('/')
			.append(snapshot.getLatencyMax()).append(')');
	}

	private void formatMultiSnapshot(final StringBuilder buffer) {
		final StrBuilder strb = new StrBuilder("metrics:");
		if(snapshots.size() > 0) {
			strb.appendNewLine();
			for(final String tableHeaderLine : TABLE_HEADER_LINES) {
				strb.append(tableHeaderLine).appendNewLine();
			}
			IoStats.Snapshot snapshot;
			long nextSuccCount;
			for(final int ioTypeCode : snapshots.keySet()) {
				snapshot = snapshots.get(ioTypeCode);
				nextSuccCount = snapshot.getSuccCount();
				strb
					.appendFixedWidthPadLeft(IoType.values()[ioTypeCode].name(), 6, ' ')
					.append('|');
				strb.appendFixedWidthPadLeft(concurrencyMap.get(ioTypeCode), 7, ' ').append('|');
				strb.appendFixedWidthPadLeft(driversCountMap.get(ioTypeCode), 7, ' ').append('|');
				strb.appendFixedWidthPadLeft(nextSuccCount, 12, ' ').append(('|'));
				strb
					.appendFixedWidthPadLeft(
						getColoredFailCountText(nextSuccCount, snapshot.getFailCount()), 6, ' '
					)
					.append('|');
				strb
					.appendFixedWidthPadLeft(
						formatFixedWidth(snapshot.getElapsedTime() / 1000.0, 7), 7, ' '
					)
					.append('|');
				strb.appendFixedWidthPadRight(snapshot.getSuccRateMean(), 8, ' ').append('|');
				strb.appendFixedWidthPadRight(snapshot.getSuccRateLast(), 8, ' ').append('|');
				strb
					.appendFixedWidthPadLeft(formatFixedSize(snapshot.getByteCount()), 8, ' ')
					.append('|');
				strb.appendFixedWidthPadRight(snapshot.getByteRateMean() / MIB, 6, ' ').append('|');
				strb.appendFixedWidthPadRight(snapshot.getByteRateLast() / MIB, 6, ' ').append('|');
				strb.appendFixedWidthPadLeft((long) snapshot.getLatencyAvg(), 12, ' ').append('|');
				strb.appendFixedWidthPadLeft((long) snapshot.getDurationAvg(), 12, ' ');
				strb.appendNewLine();
			}
			strb.appendPadding(118, '-');
		} else {
			strb.append(" not available yet");
		}
		buffer.append(strb.toString());
	}

	private static String formatFixedWidth(final double value, final int count) {
		final String valueStr = Double.toString(value);
		if(valueStr.length() > count) {
			return valueStr.substring(0, count);
		} else {
			return valueStr;
		}
	}
}
