package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.load.LoadController;
import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.log.LogMessageBase;
import static com.emc.mongoose.ui.log.LogUtil.RESET;
import static com.emc.mongoose.common.Constants.MIB;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.ui.log.LogUtil.getFailureRatioAnsiColorCode;

import org.apache.commons.lang.text.StrBuilder;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;

import java.util.Map;
import java.util.SortedSet;

/**
 Created by kurila on 18.05.17.
 */
public class MetricsAsciiTableLogMessage
extends LogMessageBase {
	
	public static final String TABLE_HEADER =
		"----------------------------------------------------------------------------------------------------" + LINE_SEPARATOR +
		"    Step    |Operat| Concur|Drive|       Count       | Step |   Last Rate    |  Mean    |   Mean    " + LINE_SEPARATOR +
		"    Name    | ion  | rency | rs  |-------------------| Time |----------------| Latency  | Duration  " + LINE_SEPARATOR +
		"            | Type |       |Count|   Success  |Failed| [s]  | [op/s] |[MB/s] |  [us]    |   [us]    " + LINE_SEPARATOR +
		"------------|------|-------|-----|------------|------|------|--------|-------|----------|-----------" + LINE_SEPARATOR;
	public static final String TABLE_BORDER_BOTTOM =
		"----------------------------------------------------------------------------------------------------";
	public static final String TABLE_BORDER_VERTICAL = "|";
	
	private final Map<LoadController, SortedSet<MetricsContext>> metrics;
	
	public MetricsAsciiTableLogMessage(
		final Map<LoadController, SortedSet<MetricsContext>> metrics
	) {
		this.metrics = metrics;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		final StrBuilder strb = new StrBuilder().appendNewLine().append(TABLE_HEADER);
		MetricsContext.Snapshot snapshot;
		long succCount;
		long failCount;
		IoType ioType;
		for(final LoadController loadController : metrics.keySet()) {
			for(final MetricsContext metricsContext : metrics.get(loadController)) {
				snapshot = metricsContext.getLastSnapshot();
				succCount = snapshot.getSuccCount();
				failCount = snapshot.getFailCount();
				ioType = metricsContext.getIoType();
				strb.appendFixedWidthPadLeft(metricsContext.getStepName(), 12, ' ')
					.append(TABLE_BORDER_VERTICAL);
				switch(ioType) {
					case NOOP:
						strb.append(LogUtil.NOOP_COLOR);
						break;
					case CREATE:
						strb.append(LogUtil.CREATE_COLOR);
						break;
					case READ:
						strb.append(LogUtil.READ_COLOR);
						break;
					case UPDATE:
						strb.append(LogUtil.UPDATE_COLOR);
						break;
					case DELETE:
						strb.append(LogUtil.DELETE_COLOR);
						break;
					case LIST:
						strb.append(LogUtil.LIST_COLOR);
						break;
				}
				strb.appendFixedWidthPadRight(metricsContext.getIoType().name(), 6, ' ')
					.append(RESET).append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadLeft(metricsContext.getConcurrency(), 7, ' ')
					.append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadLeft(metricsContext.getDriverCount(), 5, ' ')
					.append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadLeft(succCount, 12, ' ').append(TABLE_BORDER_VERTICAL);
				strb
					.append(getFailureRatioAnsiColorCode(succCount, failCount))
					.appendFixedWidthPadLeft(failCount, 6, ' ')
					.append(RESET).append(TABLE_BORDER_VERTICAL);
				strb
					.appendFixedWidthPadLeft(
						formatFixedWidth(snapshot.getElapsedTime() / 1000.0, 6), 6, ' '
					)
					.append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadRight(snapshot.getSuccRateLast(), 8, ' ')
					.append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadRight(snapshot.getByteRateLast() / MIB, 7, ' ')
					.append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadLeft((long) snapshot.getLatencyMean(), 10, ' ')
					.append(TABLE_BORDER_VERTICAL);
				strb.appendFixedWidthPadLeft((long) snapshot.getDurationMean(), 11, ' ');
				strb.appendNewLine();
			}
		}
		strb.append(TABLE_BORDER_BOTTOM);
		buffer.append(strb.toString());
	}
}
