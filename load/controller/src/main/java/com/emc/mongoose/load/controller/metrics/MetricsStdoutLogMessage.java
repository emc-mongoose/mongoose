package com.emc.mongoose.load.controller.metrics;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.LogMessageBase;
import static com.emc.mongoose.common.Constants.K;
import static com.emc.mongoose.common.Constants.M;
import static com.emc.mongoose.common.Constants.MIB;
import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;
import com.emc.mongoose.ui.log.LogUtil;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.commons.lang.text.StrBuilder;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;

/**
 Created by kurila on 26.10.16.
 */
public final class MetricsStdoutLogMessage
extends LogMessageBase {
	
	public static final String TABLE_BORDER =
		"______________________________________________________________________________________________________________________";
	public static final String TABLE_HEADER =
		TABLE_BORDER + LINE_SEPARATOR +
		" Load | Concur| Driver|       Count       |  Job  |    TP [op/s]    |        |  BW [MB/s]  |Latency [us]|Duration [us]" + LINE_SEPARATOR +
		" Type | rency | Count |-------------------| Time  |-----------------|  Size  |-------------|------------|-------------" + LINE_SEPARATOR +
		"      |       |       |   Success  |Failed|  [s]  |  Mean  |  Last  |        | Mean | Last |    Mean    |    Mean     " + LINE_SEPARATOR +
		"------|-------|-------|------------|------|-------|--------|--------|--------|------|------|------------|-------------" + LINE_SEPARATOR;
	;
	
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

	private static void formatSingleSnapshot(
		final StringBuilder buffer, final String runId, final int ioTypeCode,
		final IoStats.Snapshot snapshot, final int concurrency, final int driversCount
	) {
		final long succCount = snapshot.getSuccCount();
		final long failCount = snapshot.getFailCount();
		buffer
			.append("\n\t")
			.append(IoType.values()[ioTypeCode]).append('-')
			.append(concurrency).append('x').append(driversCount)
			.append(": n=(").append(LogUtil.WHITE).append(succCount).append(LogUtil.RESET).append('/')
			.append(failCount > 0 ? (succCount / failCount > 100 ? LogUtil.YELLOW : LogUtil.RED) : LogUtil.GREEN)
			.append(failCount).append(LogUtil.RESET).append("); t[s]=(")
			.append(formatFixedWidth(snapshot.getElapsedTime() / K, 7)).append('/')
			.append(formatFixedWidth(snapshot.getDurationSum() / M, 7)).append("); size=(")
			.append(formatFixedSize(snapshot.getByteCount())).append("); TP[op/s]=(")
			.append(formatFixedWidth(snapshot.getSuccRateMean(), 7)).append('/')
			.append(formatFixedWidth(snapshot.getSuccRateLast(), 7)).append("); BW[MB/s]=(")
			.append(formatFixedWidth(snapshot.getByteRateMean() / MIB, 6)).append('/')
			.append(formatFixedWidth(snapshot.getByteRateLast() / MIB, 6)).append("); dur[us]=(")
			.append((long) snapshot.getDurationMean()).append('/')
			.append(snapshot.getDurationMin()).append('/')
			.append(snapshot.getDurationMax()).append("); lat[us]=(")
			.append((long) snapshot.getLatencyMean()).append('/')
			.append(snapshot.getLatencyMin()).append('/')
			.append(snapshot.getLatencyMax()).append(')');
	}

	private void formatMultiSnapshot(final StringBuilder buffer) {
		final StrBuilder strb = new StrBuilder("metrics:");
		if(snapshots.size() > 0) {
			strb.appendNewLine().append(TABLE_HEADER);
			IoStats.Snapshot snapshot;
			long succCount;
			long failCount;
			for(final int ioTypeCode : snapshots.keySet()) {
				snapshot = snapshots.get(ioTypeCode);
				succCount = snapshot.getSuccCount();
				failCount = snapshot.getFailCount();
				strb
					.appendFixedWidthPadLeft(IoType.values()[ioTypeCode].name(), 6, ' ')
					.append('|');
				strb.appendFixedWidthPadLeft(concurrencyMap.get(ioTypeCode), 7, ' ').append('|');
				strb.appendFixedWidthPadLeft(driversCountMap.get(ioTypeCode), 7, ' ').append('|');
				strb
					.append(LogUtil.WHITE).appendFixedWidthPadLeft(succCount, 12, ' ')
					.append(LogUtil.RESET).append(('|'));
				strb
					.append(failCount > 0 ? (succCount / failCount > 100 ? LogUtil.YELLOW : LogUtil.RED) : LogUtil.GREEN)
					.appendFixedWidthPadLeft(failCount, 6, ' ')
					.append(LogUtil.RESET)
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
				strb.appendFixedWidthPadLeft((long) snapshot.getLatencyMean(), 12, ' ').append('|');
				strb.appendFixedWidthPadLeft((long) snapshot.getDurationMean(), 12, ' ');
				strb.appendNewLine();
			}
			strb.append(TABLE_BORDER);
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
