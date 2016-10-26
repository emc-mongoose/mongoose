package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.ui.log.MessageBase;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 26.10.16.
 
 TypeLoad,
 CountSucc,
 CountFail,
 Size,
 JobDuration[s],
 DurationSum[s],
 TPAvg[op/s],
 TPLast[op/s],
 BWAvg[MB/s],
 BWLast[MB/s],
 DurationMin[us],
 DurationLoQ[us],
 DurationAvg[us],
 DurationHiQ[us],
 DurationMax[us],
 LatencyMin[us],
 LatencyLoQ[us],
 LatencyAvg[us],
 LatencyHiQ[us],
 LatencyMax[us]
 */
public final class MetricsLogMessageCsv
extends MessageBase {
	
	private final Map<LoadType, IoStats.Snapshot> snapshots;
	
	public MetricsLogMessageCsv(final Map<LoadType, IoStats.Snapshot> snapshots) {
		this.snapshots = snapshots;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		IoStats.Snapshot nextSnapshot;
		for(final LoadType nextLoadType : snapshots.keySet()) {
			nextSnapshot = snapshots.get(nextLoadType);
			buffer
				.append(nextLoadType.name()).append(',')
				.append(nextSnapshot.getSuccCount()).append(',')
				.append(nextSnapshot.getFailCount()).append(',')
				.append(nextSnapshot.getByteCount()).append(',')
				.append(TimeUnit.MICROSECONDS.toSeconds(nextSnapshot.getElapsedTime())).append(',')
				.append(TimeUnit.MICROSECONDS.toSeconds(nextSnapshot.getDurationSum())).append(',')
				.append(nextSnapshot.getSuccRateMean()).append(',')
				.append(nextSnapshot.getSuccRateLast()).append(',')
				.append(nextSnapshot.getByteRateMean()).append(',')
				.append(nextSnapshot.getByteRateLast()).append(',')
				.append(nextSnapshot.getDurationMin()).append(',')
				.append(nextSnapshot.getDurationLoQ()).append(',')
				.append(nextSnapshot.getDurationMed()).append(',')
				.append(nextSnapshot.getDurationHiQ()).append(',')
				.append(nextSnapshot.getDurationMax()).append(',')
				.append(nextSnapshot.getLatencyMin()).append(',')
				.append(nextSnapshot.getLatencyLoQ()).append(',')
				.append(nextSnapshot.getLatencyMed()).append(',')
				.append(nextSnapshot.getLatencyHiQ()).append(',')
				.append(nextSnapshot.getLatencyMax()).append('\n');
		}
	}
}
