package com.emc.mongoose.logging;

import com.emc.mongoose.metrics.AggregatingMetricsContext;
import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.item.op.OpType;
import static com.emc.mongoose.Constants.K;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.Constants.MIB;
import static com.emc.mongoose.logging.LogUtil.RESET;
import static com.emc.mongoose.logging.LogUtil.WHITE;
import static com.emc.mongoose.logging.LogUtil.getFailureRatioAnsiColorCode;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import static com.github.akurilov.commons.system.SizeInBytes.formatFixedSize;

/**
 Created by kurila on 18.05.17.
 */
@AsynchronouslyFormattable
public final class StepResultsMetricsLogMessage
extends LogMessageBase {
	
	private final MetricsContext metricsCtx;
	
	public StepResultsMetricsLogMessage(final MetricsContext metricsCtx) {
		this.metricsCtx = metricsCtx;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		final MetricsSnapshot snapshot = metricsCtx.lastSnapshot();
		final long succCount = snapshot.succCount();
		final long failCount = snapshot.failCount();
		final OpType opType = metricsCtx.opType();
		final boolean stdOutColorFlag = metricsCtx.stdOutColorEnabled();
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
		buffer
			.append(metricsCtx instanceof AggregatingMetricsContext ? "Distributed" : "Local")
			.append(" load step ")
			.append(metricsCtx instanceof AggregatingMetricsContext ? "\"" : "slice \"")
			.append(metricsCtx.stepId())
			.append("\" results:\n\t");
		if(stdOutColorFlag) {
			buffer.append(opTypeColorCode);
		}
		buffer.append(metricsCtx.opType().name());
		if(stdOutColorFlag) {
			buffer.append(RESET);
		}
		buffer
			.append('-').append(metricsCtx.concurrencyLimit())
			.append('x').append(metricsCtx.nodeCount())
			.append(": c=(").append(formatFixedWidth(snapshot.actualConcurrencyMean(), 6))
			.append("); n=(");
		if(stdOutColorFlag) {
			buffer.append(WHITE);
		}
		buffer.append(succCount);
		if(stdOutColorFlag) {
			buffer.append(RESET);
		}
		buffer.append('/');
		if(stdOutColorFlag) {
			buffer.append(getFailureRatioAnsiColorCode(succCount, failCount));
		}
		buffer.append(failCount);
		if(stdOutColorFlag) {
			buffer.append(RESET);
		}
		buffer
			.append("); t[s]=(")
			.append(formatFixedWidth(snapshot.elapsedTimeMillis() / K, 7)).append('/')
			.append(formatFixedWidth(snapshot.durationSum() / M, 7)).append("); size=(")
			.append(formatFixedSize(snapshot.byteCount())).append("); TP[op/s]=(")
			.append(formatFixedWidth(snapshot.succRateMean(), 7)).append('/')
			.append(formatFixedWidth(snapshot.succRateLast(), 7)).append("); BW[MB/s]=(")
			.append(formatFixedWidth(snapshot.byteRateMean() / MIB, 6)).append('/')
			.append(formatFixedWidth(snapshot.byteRateLast() / MIB, 6)).append("); dur[us]=(")
			.append((long) snapshot.durationMean()).append('/')
			.append(snapshot.durationMin()).append('/')
			.append(snapshot.durationMax()).append("); lat[us]=(")
			.append((long) snapshot.latencyMean()).append('/')
			.append(snapshot.latencyMin()).append('/')
			.append(snapshot.latencyMax()).append(')')
			.append(System.lineSeparator());
	}
}
