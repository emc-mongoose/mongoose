package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.ui.log.MessageBase;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 26.10.16.
 
 TypeLoad,
 TotalConcurrency,
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
	private final int totalConcurrency;
	
	public MetricsLogMessageCsv(
		final Map<LoadType, IoStats.Snapshot> snapshots, final int totalConcurrency
	) {
		this.snapshots = snapshots;
		this.totalConcurrency = totalConcurrency;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		final Iterator<Map.Entry<LoadType, IoStats.Snapshot>>
			entryIter = snapshots.entrySet().iterator();
		Map.Entry<LoadType, IoStats.Snapshot> nextEntry;
		IoStats.Snapshot nextSnapshot;
		while(entryIter.hasNext()) {
			nextEntry = entryIter.next();
			nextSnapshot = nextEntry.getValue();
			buffer
				.append(nextEntry.getKey().name()).append(',')
				.append(totalConcurrency).append(',')
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
				.append(nextSnapshot.getLatencyHiQ());
			if(entryIter.hasNext()) {
				buffer.append(nextSnapshot.getLatencyMax()).append('\n');
			} else {
				break;
			}
		}
	}
}
