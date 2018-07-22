package com.emc.mongoose.logging;

import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.item.op.OpType;
import static com.emc.mongoose.env.DateUtil.FMT_DATE_METRICS_TABLE;
import static com.emc.mongoose.logging.LogUtil.RESET;
import static com.emc.mongoose.Constants.MIB;
import static com.emc.mongoose.logging.LogUtil.getFailureRatioAnsiColorCode;

import org.apache.commons.lang.text.StrBuilder;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;

import java.util.Date;
import java.util.Set;

/**
 Created by kurila on 18.05.17.
 Not thread safe, relies on the MetricsManager's (caller) exclusive invocation lock
 */
public class MetricsAsciiTableLogMessage
extends LogMessageBase {

	public static final String TABLE_HEADER =
		"------------------------------------------------------------------------------------------------------------------------" + LINE_SEPARATOR +
		" Step Id  | Timestamp  |  Op  |     Concurrency     |       Count       | Step  |   Last Rate    |  Mean    |   Mean    " + LINE_SEPARATOR +
		" (last 10 |            | type |---------------------|-------------------| Time  |----------------| Latency  | Duration  " + LINE_SEPARATOR +
		" symbols) |yyMMddHHmmss|      | Current  |   Mean   |   Success  |Failed|  [s]  | [op/s] |[MB/s] |  [us]    |   [us]    " + LINE_SEPARATOR +
		"----------|------------|------|----------|----------|------------|------|-------|--------|-------|----------|-----------" + LINE_SEPARATOR;
	public static final String SUMMARY_DELIMETER =
		"************************************************************************************************************************" + LINE_SEPARATOR;
	public static final String TABLE_BORDER_VERTICAL = "|";
	public static final int TABLE_HEADER_PERIOD = 20;

	private static volatile long ROW_OUTPUT_COUNTER = 0;

	private final Set<MetricsContext> metrics;
	private final boolean summaryFlag;

	private volatile String formattedMsg = null;
	
	public MetricsAsciiTableLogMessage(final Set<MetricsContext> metrics) {
		this(metrics, false);
	}

	public MetricsAsciiTableLogMessage(final Set<MetricsContext> metrics, final boolean summaryFlag) {
		this.metrics = metrics;
		this.summaryFlag = summaryFlag;
	}
	
	@Override
	public final void formatTo(final StringBuilder buffer) {
		if(formattedMsg == null) {
			final StrBuilder strb = new StrBuilder();
			MetricsSnapshot snapshot;
			long succCount;
			long failCount;
			OpType opType;
			boolean stdOutColorFlag;
			for(final MetricsContext metricsCtx : metrics) {
				snapshot = metricsCtx.lastSnapshot();
				succCount = snapshot.succCount();
				failCount = snapshot.failCount();
				opType = metricsCtx.ioType();
				stdOutColorFlag = metricsCtx.stdOutColorEnabled();
				if(0 == ROW_OUTPUT_COUNTER % TABLE_HEADER_PERIOD) {
					strb.append(TABLE_HEADER);
				}
				if(summaryFlag) {
					strb.append(SUMMARY_DELIMETER);
				}
				ROW_OUTPUT_COUNTER ++;
				strb
					.appendFixedWidthPadLeft(metricsCtx.stepId(), 10, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadLeft(FMT_DATE_METRICS_TABLE.format(new Date()), 12, ' ')
					.append(TABLE_BORDER_VERTICAL);
				if(stdOutColorFlag) {
					switch(opType) {
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
				strb.appendFixedWidthPadRight(metricsCtx.ioType().name(), 6, ' ');
				if(stdOutColorFlag) {
					strb.append(RESET);
				}
				strb
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadLeft(snapshot.actualConcurrencyLast(), 10, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadRight(formatFixedWidth(snapshot.actualConcurrencyMean(), 10), 10, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadLeft(succCount, 12, ' ').append(TABLE_BORDER_VERTICAL);
				if(stdOutColorFlag) {
					strb.append(getFailureRatioAnsiColorCode(succCount, failCount));
				}
				strb.appendFixedWidthPadLeft(failCount, 6, ' ');
				if(stdOutColorFlag) {
					strb.append(RESET);
				}
				strb
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadRight((double) snapshot.elapsedTimeMillis() / 1000, 7, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadRight(formatFixedWidth(snapshot.succRateLast(), 8), 8, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadRight(formatFixedWidth(snapshot.byteRateLast() / MIB, 7), 7, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadLeft((long) snapshot.latencyMean(), 10, ' ')
					.append(TABLE_BORDER_VERTICAL)
					.appendFixedWidthPadLeft((long) snapshot.durationMean(), 11, ' ')
					.appendNewLine();
				if(summaryFlag) {
					strb.append(SUMMARY_DELIMETER);
				}
			}
			formattedMsg = strb.toString();
		}
		buffer.append(formattedMsg);
	}
}
