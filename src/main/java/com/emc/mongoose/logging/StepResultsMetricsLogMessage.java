package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsContext;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.Constants.MIB;
import static com.emc.mongoose.logging.LogUtil.RESET;
import static com.emc.mongoose.logging.LogUtil.WHITE;
import static com.emc.mongoose.logging.LogUtil.getFailureRatioAnsiColorCode;

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
	private final boolean stdOutColorFlag;
	private final String stepId;
	private final DistributedMetricsSnapshot snapshot;

	public StepResultsMetricsLogMessage(final DistributedMetricsContext<DistributedMetricsSnapshot> metricsCtx) {
		this(metricsCtx.opType(), metricsCtx.stdOutColorEnabled(), metricsCtx.stepId(), metricsCtx.lastSnapshot());
	}

	StepResultsMetricsLogMessage(
		final OpType opType, final boolean stdOutColorFlag, final String stepId,
		final DistributedMetricsSnapshot snapshot
	) {
		this.opType = opType;
		this.stdOutColorFlag = stdOutColorFlag;
		this.stepId = stepId;
		this.snapshot = snapshot;
	}

	@Override
	public final void formatTo(final StringBuilder buff) {

		final long succCount = snapshot.succCount();
		final long failCount = snapshot.failCount();
		String opTypeColorCode = WHITE;
		switch(opType) {
			case NOOP:
				opTypeColorCode = LogUtil.NOOP_COLOR;
				break;
			case CREATE:
				opTypeColorCode = LogUtil.CREATE_COLOR;
				break;
			case READ:
				opTypeColorCode = LogUtil.READ_COLOR;
				break;
			case UPDATE:
				opTypeColorCode = LogUtil.UPDATE_COLOR;
				break;
			case DELETE:
				opTypeColorCode = LogUtil.DELETE_COLOR;
				break;
			case LIST:
				opTypeColorCode = LogUtil.LIST_COLOR;
				break;
		}

		final String lineSep = System.lineSeparator();
		buff
			.append("---").append(lineSep)
			.append("- Load Step Id:                 ").append(stepId).append(lineSep)

			.append("    Operation Type:             ");
		if(stdOutColorFlag) {
			buff.append(opTypeColorCode);
		}
		buff.append(opType);
		if(stdOutColorFlag) {
			buff.append(RESET);
		}
		buff.append(lineSep);

		buff
			.append("    Node Count:                 ").append(snapshot.nodeCount()).append(lineSep)
			.append("    Concurrency:                ").append(lineSep)
			.append("      Limit Per Storage Driver: ").append(snapshot.concurrencyLimit()).append(lineSep)
			.append("      Actual:                   ").append(lineSep)
			.append("        Last:                   ").append(snapshot.actualConcurrencyLast()).append(lineSep)
			.append("        Mean:                   ").append(snapshot.actualConcurrencyMean()).append(lineSep)
			.append("    Operations Count:           ").append(lineSep)
			.append("      Successful:               ");

		if(stdOutColorFlag) {
			buff.append(WHITE);
		}
		buff.append(snapshot.succCount());
		if(stdOutColorFlag) {
			buff.append(RESET);
		}
		buff.append(lineSep);

		buff.append("      Failed:                   ");
		if(stdOutColorFlag) {
			buff.append(getFailureRatioAnsiColorCode(succCount, failCount));
		}
		buff.append(snapshot.failCount());
		if(stdOutColorFlag) {
			buff.append(RESET);
		}
		buff.append(lineSep);

		buff
			.append("    Transfer Size:              ").append(SizeInBytes.formatFixedSize(snapshot.byteCount()))
			.append(lineSep)
			.append("    Duration [s]:               ").append(lineSep)
			.append("      Elapsed:                  ")
			.append(TimeUnit.MILLISECONDS.toSeconds(snapshot.elapsedTimeMillis())).append(lineSep)
			.append("      Sum:                      ").append(snapshot.durationSum() / M).append(lineSep)
			.append("    Throughput [op/s]:          ").append(lineSep)
			.append("      Last:                     ").append(snapshot.succRateLast()).append(lineSep)
			.append("      Mean:                     ").append(snapshot.succRateMean()).append(lineSep)
			.append("    Bandwidth [MB/s]:           ").append(lineSep)
			.append("      Last:                     ").append(snapshot.byteRateLast() / MIB).append(lineSep)
			.append("      Mean:                     ").append(snapshot.byteRateMean() / MIB).append(lineSep)
			.append("    Operations Duration [us]:   ").append(lineSep)
			.append("      Avg:                      ").append(snapshot.durationMean()).append(lineSep)
			.append("      Min:                      ").append(snapshot.durationMin()).append(lineSep)
			.append("      LoQ:                      ").append(snapshot.durationLoQ()).append(lineSep)
			.append("      Med:                      ").append(snapshot.durationMed()).append(lineSep)
			.append("      HiQ:                      ").append(snapshot.durationHiQ()).append(lineSep)
			.append("      Max:                      ").append(snapshot.durationMax()).append(lineSep)
			.append("    Operations Latency [us]:    ").append(lineSep)
			.append("      Avg:                      ").append(snapshot.latencyMean()).append(lineSep)
			.append("      Min:                      ").append(snapshot.latencyMin()).append(lineSep)
			.append("      LoQ:                      ").append(snapshot.latencyLoQ()).append(lineSep)
			.append("      Med:                      ").append(snapshot.latencyMed()).append(lineSep)
			.append("      HiQ:                      ").append(snapshot.latencyHiQ()).append(lineSep)
			.append("      Max:                      ").append(snapshot.latencyMax()).append(lineSep)
			.append("---").append(lineSep);
	}
}
