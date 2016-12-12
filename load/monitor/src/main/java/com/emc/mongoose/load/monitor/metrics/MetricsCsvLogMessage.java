package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.LogMessageBase;
import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_ISO8601;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.Date;
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
extends LogMessageBase {
	
	private final Int2ObjectMap<IoStats.Snapshot> snapshots;
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;
	
	public MetricsCsvLogMessage(
		final Int2ObjectMap<IoStats.Snapshot> snapshots, final Int2IntMap concurrencyMap,
		final Int2IntMap driversCountMap
	) {
		this.snapshots = snapshots;
		this.concurrencyMap = concurrencyMap;
		this.driversCountMap = driversCountMap;
	}
	
	@Override
	public final void formatTo(final StringBuilder strb) {
		final Iterator<Map.Entry<Integer, IoStats.Snapshot>>
			entryIter = snapshots.entrySet().iterator();
		Map.Entry<Integer, IoStats.Snapshot> nextEntry;
		IoStats.Snapshot nextSnapshot;
		final Date current = new Date();
		while(entryIter.hasNext()) {
			nextEntry = entryIter.next();
			nextSnapshot = nextEntry.getValue();
			strb
				.append('"').append(FMT_DATE_ISO8601.format(current)).append('"').append(',')
				.append(IoType.values()[nextEntry.getKey()].name()).append(',')
				.append(concurrencyMap.get(nextEntry.getKey())).append('x')
				.append(driversCountMap.get(nextEntry.getKey())).append(',')
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
				.append(nextSnapshot.getLatencyMax());
			if(entryIter.hasNext()) {
				strb.append('\n');
			} else {
				break;
			}
		}
	}
}
