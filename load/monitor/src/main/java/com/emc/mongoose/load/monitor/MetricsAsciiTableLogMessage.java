package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.log.LogMessageBase;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.MIB;

import org.apache.commons.lang.text.StrBuilder;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;

import java.util.SortedSet;

/**
 Created by kurila on 18.05.17.
 */
public class MetricsAsciiTableLogMessage
extends LogMessageBase {
	
	public static final String TABLE_HEADER =
		"┌────────────┬──────┬───────┬──────┬───────────────────┬──────┬────────────────┬──────────┬───────────┐" + LINE_SEPARATOR +
		"│    Step    │  Op  │ Concur│Driver│       Count       │ Step │   Last Rate    │  Mean    │   Mean    │" + LINE_SEPARATOR +
		"│    Name    │ Type │ rency │Count ├────────────┬──────┤ Time ├────────┬───────┤ Latency  │ Duration  │" + LINE_SEPARATOR +
		"│            │      │       │      │   Success  │Failed│ [s]  │ [op/s] │[MB/s] │  [us]    │   [us]    │" + LINE_SEPARATOR;
	public static final String TABLE_BORDER_ROW =
		"├────────────┼──────┼───────┼──────┼────────────┼──────┼──────┼────────┼───────┼──────────┼───────────┤" + LINE_SEPARATOR;
	public static final String TABLE_BORDER_BOTTOM =
		"└────────────┴──────┴───────┴──────┴────────────┴──────┴──────┴────────┴───────┴──────────┴───────────┘";
	
	private final SortedSet<MetricsContext> metrics;
	
	public MetricsAsciiTableLogMessage(final SortedSet<MetricsContext> metrics) {
		this.metrics = metrics;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		if(metrics != null && metrics.size() > 0) {
			final StrBuilder strb = new StrBuilder().appendNewLine().append(TABLE_HEADER);
			MetricsContext.Snapshot snapshot;
			long succCount;
			long failCount;
			for(final MetricsContext metricsContext : metrics) {
				snapshot = metricsContext.getLastSnapshot();
				succCount = snapshot.getSuccCount();
				failCount = snapshot.getFailCount();
				strb.append(TABLE_BORDER_ROW).append('│');
				strb.appendFixedWidthPadLeft(metricsContext.getStepName(), 12, ' ').append('│');
				strb.appendFixedWidthPadLeft(metricsContext.getIoType().name(), 6, ' ').append('│');
				strb.appendFixedWidthPadLeft(metricsContext.getConcurrency(), 7, ' ').append('│');
				strb.appendFixedWidthPadLeft(metricsContext.getDriverCount(), 6, ' ').append('│');
				strb
					.append(LogUtil.WHITE).appendFixedWidthPadLeft(succCount, 12, ' ')
					.append(LogUtil.RESET).append(('│'));
				strb
					.append(failCount > 0 ? (succCount / failCount > 100 ? LogUtil.YELLOW : LogUtil.RED) : LogUtil.GREEN)
					.appendFixedWidthPadLeft(failCount, 6, ' ')
					.append(LogUtil.RESET)
					.append('│');
				strb
					.appendFixedWidthPadLeft(
						formatFixedWidth(snapshot.getElapsedTime() / 1000.0, 6), 6, ' '
					)
					.append('│');
				strb.appendFixedWidthPadRight(snapshot.getSuccRateLast(), 8, ' ').append('│');
				strb.appendFixedWidthPadRight(snapshot.getByteRateLast() / MIB, 7, ' ').append('│');
				strb.appendFixedWidthPadLeft((long) snapshot.getLatencyMean(), 10, ' ').append('│');
				strb.appendFixedWidthPadLeft((long) snapshot.getDurationMean(), 11, ' ').append('│');
				strb.appendNewLine();
			}
			strb.append(TABLE_BORDER_BOTTOM);
			buffer.append(strb.toString());
		} else {
			buffer.append("Metrics not available yet");
		}
	}
}
