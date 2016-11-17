package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.MessageBase;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

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
public final class MetricsCsvLogMessage
extends MessageBase {
	
	private final Int2ObjectMap<IoStats.Snapshot> snapshots;
	private final int totalConcurrency;
	
	public MetricsCsvLogMessage(
		final Int2ObjectMap<IoStats.Snapshot> snapshots, final int totalConcurrency
	) {
		this.snapshots = snapshots;
		this.totalConcurrency = totalConcurrency;
	}
	
	@Override
	public final void formatTo(final StringBuilder strb) {
		final Iterator<Map.Entry<Integer, IoStats.Snapshot>>
			entryIter = snapshots.entrySet().iterator();
		Map.Entry<Integer, IoStats.Snapshot> nextEntry;
		IoStats.Snapshot nextSnapshot;
		while(entryIter.hasNext()) {
			nextEntry = entryIter.next();
			nextSnapshot = nextEntry.getValue();
			strb
				.append(IoType.values()[nextEntry.getKey()].name()).append(',')
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
				strb.append(nextSnapshot.getLatencyMax()).append('\n');
			} else {
				break;
			}
		}
	}
}
