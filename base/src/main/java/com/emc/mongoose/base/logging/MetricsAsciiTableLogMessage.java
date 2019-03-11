package com.emc.mongoose.base.logging;

import static com.emc.mongoose.base.Constants.MIB;
import static com.emc.mongoose.base.env.DateUtil.FMT_DATE_METRICS_TABLE;
import static com.emc.mongoose.base.logging.LogUtil.RESET;
import static com.emc.mongoose.base.logging.LogUtil.getFailureRatioAnsiColorCode;

import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.metrics.context.MetricsContext;
import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import java.util.Date;
import java.util.Set;
import org.apache.commons.lang.text.StrBuilder;

/**
* Created by kurila on 18.05.17. Not thread safe, relies on the MetricsManager's (caller) exclusive
* invocation lock
*/
public class MetricsAsciiTableLogMessage extends LogMessageBase {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	public static final String TABLE_HEADER = "------------------------------------------------------------------------------------------------------------------------"
					+ LINE_SEPARATOR
					+ " Step Id  | Timestamp  |  Op  |     Concurrency     |       Count       | Step  |   Last Rate    |  Mean    |   Mean    "
					+ LINE_SEPARATOR
					+ " (last 10 |            | type |---------------------|-------------------| Time  |----------------| Latency  | Duration  "
					+ LINE_SEPARATOR
					+ " symbols) |yyMMddHHmmss|      | Current  |   Mean   |   Success  |Failed|  [s]  | [op/s] |[MB/s] |  [us]    |   [us]    "
					+ LINE_SEPARATOR
					+ "----------|------------|------|----------|----------|------------|------|-------|--------|-------|----------|-----------"
					+ LINE_SEPARATOR;
	public static final String TABLE_BORDER_VERTICAL = "|";
	public static final int TABLE_HEADER_PERIOD = 20;
	private static volatile long ROW_OUTPUT_COUNTER = 0;
	private final Set<MetricsContext> metrics;
	private volatile String formattedMsg = null;

	public MetricsAsciiTableLogMessage(final Set<MetricsContext> metrics) {
		this.metrics = metrics;
	}

	@Override
	public final void formatTo(final StringBuilder buffer) {
		if (formattedMsg == null) {
			final var strb = new StrBuilder();
			AllMetricsSnapshot snapshot;
			long succCount;
			long failCount;
			OpType opType;
			boolean stdOutColorFlag;
			for (final var metricsCtx : metrics) {
				snapshot = metricsCtx.lastSnapshot();
				if (snapshot != null) {
					succCount = snapshot.successSnapshot().count();
					failCount = snapshot.failsSnapshot().count();
					opType = metricsCtx.opType();
					stdOutColorFlag = metricsCtx.stdOutColorEnabled();
					if (0 == ROW_OUTPUT_COUNTER % TABLE_HEADER_PERIOD) {
						strb.append(TABLE_HEADER);
					}
					ROW_OUTPUT_COUNTER++;
					strb.appendFixedWidthPadLeft(metricsCtx.id(), 10, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadLeft(FMT_DATE_METRICS_TABLE.format(new Date()), 12, ' ')
									.append(TABLE_BORDER_VERTICAL);
					if (stdOutColorFlag) {
						switch (opType) {
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
					}
					strb.appendFixedWidthPadRight(metricsCtx.opType().name(), 6, ' ');
					if (stdOutColorFlag) {
						strb.append(RESET);
					}
					strb.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadLeft(snapshot.concurrencySnapshot().last(), 10, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadRight(
													formatFixedWidth(snapshot.concurrencySnapshot().mean(), 10), 10, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadLeft(succCount, 12, ' ')
									.append(TABLE_BORDER_VERTICAL);
					if (stdOutColorFlag) {
						strb.append(getFailureRatioAnsiColorCode(succCount, failCount));
					}
					strb.appendFixedWidthPadLeft(failCount, 6, ' ');
					if (stdOutColorFlag) {
						strb.append(RESET);
					}
					strb.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadRight((double) snapshot.elapsedTimeMillis() / 1000, 7, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadRight(
													formatFixedWidth(snapshot.successSnapshot().last(), 8), 8, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadRight(
													formatFixedWidth(snapshot.byteSnapshot().last() / MIB, 7), 7, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadLeft((long) snapshot.latencySnapshot().mean(), 10, ' ')
									.append(TABLE_BORDER_VERTICAL)
									.appendFixedWidthPadLeft((long) snapshot.durationSnapshot().mean(), 11, ' ')
									.appendNewLine();
				}
			}
			formattedMsg = strb.toString();
		}
		buffer.append(formattedMsg);
	}
}
