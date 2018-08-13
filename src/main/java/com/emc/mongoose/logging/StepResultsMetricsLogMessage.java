package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsContext;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.Constants.MIB;

import com.github.akurilov.commons.system.SizeInBytes;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 18.05.17.
 */
@AsynchronouslyFormattable
public class StepResultsMetricsLogMessage
extends LogMessageBase {

	private final OpType opType;
	private final String stepId;
	private final DistributedMetricsSnapshot snapshot;

	public StepResultsMetricsLogMessage(final DistributedMetricsContext<DistributedMetricsSnapshot> metricsCtx) {
		this(metricsCtx.opType(), metricsCtx.stepId(), metricsCtx.lastSnapshot());
	}

	StepResultsMetricsLogMessage(final OpType opType, final String stepId, final DistributedMetricsSnapshot snapshot) {
		this.opType = opType;
		this.stepId = stepId;
		this.snapshot = snapshot;
	}

	@Override
	public final void formatTo(final StringBuilder buff) {
		final String lineSep = System.lineSeparator();
		buff
			.append("--- # Results").append(lineSep)
			.append("- Load Step Id:                ").append(stepId).append(lineSep)
			.append("  Operation Type:              ").append(opType).append(lineSep)
			.append("  Node Count:                  ").append(snapshot.nodeCount()).append(lineSep)
			.append("  Concurrency:                 ").append(lineSep)
			.append("    Limit Per Storage Driver:  ").append(snapshot.concurrencyLimit()).append(lineSep)
			.append("    Actual:                    ").append(lineSep)
			.append("      Last:                    ").append(snapshot.actualConcurrencyLast()).append(lineSep)
			.append("      Mean:                    ").append(snapshot.actualConcurrencyMean()).append(lineSep)
			.append("  Operations Count:            ").append(lineSep)
			.append("    Successful:                ").append(snapshot.succCount()).append(lineSep)
			.append("    Failed:                    ").append(snapshot.failCount()).append(lineSep)
			.append("  Transfer Size:               ").append(SizeInBytes.formatFixedSize(snapshot.byteCount())).append(lineSep)
			.append("  Duration [s]:                ").append(lineSep)
			.append("    Elapsed:                   ").append(TimeUnit.MILLISECONDS.toSeconds(snapshot.elapsedTimeMillis())).append(lineSep)
			.append("    Sum:                       ").append(snapshot.durationSum() / M).append(lineSep)
			.append("  Throughput [op/s]:           ").append(lineSep)
			.append("    Last:                      ").append(snapshot.succRateLast()).append(lineSep)
			.append("    Mean:                      ").append(snapshot.succRateMean()).append(lineSep)
			.append("  Bandwidth [MB/s]:            ").append(lineSep)
			.append("    Last:                      ").append(snapshot.byteRateLast() / MIB).append(lineSep)
			.append("    Mean:                      ").append(snapshot.byteRateMean() / MIB).append(lineSep)
			.append("  Operations Duration [us]:    ").append(lineSep)
			.append("    Avg:                       ").append(snapshot.durationMean()).append(lineSep)
			.append("    Min:                       ").append(snapshot.durationMin()).append(lineSep)
			.append("    LoQ:                       ").append(snapshot.durationLoQ()).append(lineSep)
			.append("    Med:                       ").append(snapshot.durationMed()).append(lineSep)
			.append("    HiQ:                       ").append(snapshot.durationHiQ()).append(lineSep)
			.append("    Max:                       ").append(snapshot.durationMax()).append(lineSep)
			.append("  Operations Latency [us]:     ").append(lineSep)
			.append("    Avg:                       ").append(snapshot.latencyMean()).append(lineSep)
			.append("    Min:                       ").append(snapshot.latencyMin()).append(lineSep)
			.append("    LoQ:                       ").append(snapshot.latencyLoQ()).append(lineSep)
			.append("    Med:                       ").append(snapshot.latencyMed()).append(lineSep)
			.append("    HiQ:                       ").append(snapshot.latencyHiQ()).append(lineSep)
			.append("    Max:                       ").append(snapshot.latencyMax()).append(lineSep)
			.append("---").append(lineSep);
	}
}
