package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.log.LogMessageBase;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.K;
import static com.emc.mongoose.common.Constants.M;
import static com.emc.mongoose.common.Constants.MIB;
import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;

/**
 Created by kurila on 18.05.17.
 */
public final class BasicMetricsLogMessage
extends LogMessageBase {
	
	private MetricsContext metricsCtx;
	
	public BasicMetricsLogMessage(final MetricsContext metricsCtx) {
		this.metricsCtx = metricsCtx;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		final MetricsContext.Snapshot snapshot = metricsCtx.getLastSnapshot();
		final long succCount = snapshot.getSuccCount();
		final long failCount = snapshot.getFailCount();
		buffer
			.append("\n\t")
			.append(metricsCtx.getIoType().name()).append('-')
			.append(metricsCtx.getConcurrency()).append('x').append(metricsCtx.getDriverCount())
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
}
