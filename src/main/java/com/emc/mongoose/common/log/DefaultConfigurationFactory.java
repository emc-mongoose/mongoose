package com.emc.mongoose.common.log;
//
import org.apache.logging.log4j.Level;
//
import static org.apache.logging.log4j.core.Filter.Result.ACCEPT;
import static org.apache.logging.log4j.core.Filter.Result.DENY;
import static org.apache.logging.log4j.core.Filter.Result.NEUTRAL;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.net.URI;
/**
 Created by kurila on 29.10.15.
 */
@Plugin(name = "DefaultConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class DefaultConfigurationFactory
extends ConfigurationFactory {
	//
	public static String
		HEADER_PERF_AVG_FILE = "DateTimeISO8601,LoadId,TypeAPI,TypeLoad,CountConn,CountNode,CountLoadServer,CountSucc,CountFail,DurationAvg[us],DurationMin[us],DurationMax[us],LatencyAvg[us],LatencyMin[us],LatencyMax[us],TPAvg[op/s],TPLast[op/s],BWAvg[MB/s],BWLast[MB/s]\n",
		HEADER_PERF_SUM_FILE = "DateTimeISO8601,LoadId,TypeAPI,TypeLoad,CountConn,CountNode,CountLoadServer,CountSucc,CountFail,DurationAvg[us],DurationMin[us],DurationLoQ[us],DurationMed[us],DurationHiQ[us],DurationMax[us],LatencyAvg[us],LatencyMin[us],LatencyLoQ[us],LatencyMed[us],LatencyHiQ[us],LatencyMax[us],TPAvg[op/s],TPLast[op/s],BWAvg[MB/s],BWLast[MB/s]\n",
		HEADER_PERF_TRACE_FILE = LogUtil.PERF_TRACE_HEADERS_C1,
		PATTERN_STDOUT = "%highlight{%d{ISO8601}{GMT+0} %p{length=1} %-20.-20c{1} %-40.-40t %m%n}",
		PATTERN_MSG_FILE = "%d{ISO8601}{GMT+0} | %p | %c{1} | %t | %m%n",
		PATTERN_3RD_PARTY_FILE = "%d{ISO8601}{GMT+0} | %p | %c{1} | %t | %m%n",
		PATTERN_ERR_FILE = "%d{ISO8601}{GMT+0} | %p | %c{1} | %t | %m%n",
		PATTERN_PERF_AVG_FILE = "\"%d{ISO8601}{GMT+0}\",%replace{%t}{\\w*<?([\\d]+)\\-([A-Za-z0-9]+)\\-([CreatRdDlUpAn]+)[\\d]*\\-([\\d]*)x([\\d]*)x?([\\d]*)>*[\\S]*}{$1,$2,$3,$4,$5,$6},%replace{%m}{count=\\((\\d+)/\\\u001B*\\[*\\d*m*(\\d+)\\\u001B*\\[*\\d*m*\\);[\\s]+duration\\[us\\]=\\((\\d+)/(\\d+)/(\\d+)\\);[\\s]+latency\\[us\\]=\\((\\d+)/(\\d+)/(\\d+)\\);[\\s]+TP\\[op/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\);[\\s]+BW\\[MB/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\)}{$1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12}%n",
		PATTERN_PERF_SUM_FILE = "\"%d{ISO8601}{GMT+0}\",%replace{%m}{\"([\\d]+)\\-([A-Za-z0-9]+)\\-([CreatRdDlUpAn]+)[\\d]*\\-([\\d]*)x([\\d]*)x?([\\d]*)\"[\\s]+summary:[\\s]+count=\\((\\d+)/\\\u001B*\\[*\\d*m*(\\d+)\\\u001B*\\[*\\d*m*\\);[\\s]+duration\\[us\\]=\\((\\d+)/(\\d+)/(\\d+)/(\\d+)/(\\d+)/(\\d+)\\);[\\s]+latency\\[us\\]=\\((\\d+)/(\\d+)/(\\d+)/(\\d+)/(\\d+)/(\\d+)\\);[\\s]+TP\\[op/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\);[\\s]+BW\\[MB/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\)}{$1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23,$24}%n",
		PATTERN_PERF_TRACE_FILE = "%t,%m%n",
		PATTERN_ITEM_LIST_FILE = "%m%n",
		PATTERN_CFG_FILE = "%d{ISO8601}{GMT+0} | %c{1} | %t | %m%n";
	//
	public static Configuration createConfiguration(
		final String name, final ConfigurationBuilder<BuiltConfiguration> builder
	) {
		return builder
			.setConfigurationName(name)
			.setStatusLevel(Level.OFF)
			.setPackages(
				EmbeddedConfigurationFactory.class.getPackage().getName() + ".appenders"
			)
			.setShutdownHook("enable")
			.add(
				builder
					.newAppender("stdout", "Console")
					.addAttribute("follow", true)
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("alwaysWriteExceptions", false)
							.addAttribute("pattern", PATTERN_STDOUT)
					)
					.add(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.CFG.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.ITEM_LIST.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.PERF_TRACE.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", ACCEPT, DENY)
									.addAttribute("level", Level.INFO)
							)
					)
			)
			.add(
				builder
					.newAppender("WebUI", "WebUI")
					.addAttribute("enabled", true)
					.addComponent(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.CFG.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.ITEM_LIST.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.PERF_TRACE.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", ACCEPT, DENY)
									.addAttribute("level", Level.INFO)
							)
					)
			)
			.add(
				builder
					.newAppender("msgFile", "runIdFile")
					.addAttribute("fileName", "messages.log")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("pattern", PATTERN_MSG_FILE)
					)
					.add(
						builder
							.newFilter("MarkerFilter", ACCEPT, DENY)
							.addAttribute("marker", Markers.MSG.getName())
					)
			)
			.add(
				builder
					.newAppender("3rdPartyFile", "runIdFile")
					.addAttribute("fileName", "3rdparty.log")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("pattern", PATTERN_3RD_PARTY_FILE)
					)
					.add(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.CFG.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.MSG.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.ERR.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.ITEM_LIST.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.PERF_AVG.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.PERF_SUM.getName())
							)
							.addComponent(
								builder
									.newFilter("MarkerFilter", DENY, NEUTRAL)
									.addAttribute("marker", Markers.PERF_TRACE.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", NEUTRAL, DENY)
									.addAttribute("level", Level.DEBUG)
							)
							.addComponent(
								builder
									.newFilter("BurstFilter", ACCEPT, DENY)
									.addAttribute("rate", 10)
									.addAttribute("maxBurst", 100)
							)
					)
			)
			.add(
				builder
					.newAppender("errFile", "runIdFile")
					.addAttribute("fileName", "errors.log")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("pattern", PATTERN_ERR_FILE)
					)
					.add(
						builder
							.newFilter("filters", NEUTRAL, NEUTRAL)
							.addComponent(
								builder
									.newFilter("MarkerFilter", ACCEPT, DENY)
									.addAttribute("marker", Markers.ERR.getName())
							)
							.addComponent(
								builder
									.newFilter("BurstFilter", ACCEPT, DENY)
									.addAttribute("rate", 10)
									.addAttribute("maxBurst", 100)
							)
					)
			)
			.add(
				builder
					.newAppender("perfAvgFile", "runIdFile")
					.addAttribute("fileName", "perf.avg.csv")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("header", HEADER_PERF_AVG_FILE)
							.addAttribute("pattern", PATTERN_PERF_AVG_FILE)
							.addAttribute("noConsoleNoAnsi", true)
					)
					.add(
						builder
							.newFilter("MarkerFilter", ACCEPT, DENY)
							.addAttribute("marker", Markers.PERF_AVG.getName())
					)
			)
			.add(
				builder
					.newAppender("perfSumFile", "runIdFile")
					.addAttribute("fileName", "perf.sum.csv")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("header", HEADER_PERF_SUM_FILE)
							.addAttribute("pattern", PATTERN_PERF_SUM_FILE)
							.addAttribute("noConsoleNoAnsi", true)
					)
					.add(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", ACCEPT, DENY)
									.addAttribute("marker", Markers.PERF_SUM.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", ACCEPT, DENY)
									.addAttribute("level", Level.INFO)
							)
					)
			)
			.add(
				builder
					.newAppender("perfTraceFile", "runIdFile")
					.addAttribute("fileName", "perf.trace.csv")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("header", HEADER_PERF_TRACE_FILE)
							.addAttribute("pattern", PATTERN_PERF_TRACE_FILE)
					)
					.add(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", ACCEPT, DENY)
									.addAttribute("marker", Markers.PERF_TRACE.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", ACCEPT, DENY)
									.addAttribute("level", Level.INFO)
							)
					)
			)
			.add(
				builder
					.newAppender("itemListFile", "runIdFile")
					.addAttribute("fileName", "items.csv")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("pattern", PATTERN_ITEM_LIST_FILE)
					)
					.add(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", ACCEPT, DENY)
									.addAttribute("marker", Markers.ITEM_LIST.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", ACCEPT, DENY)
									.addAttribute("level", Level.INFO)
							)
					)
			)
			.add(
				builder
					.newAppender("cfgFile", "runIdFile")
					.addAttribute("fileName", "config.log")
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("pattern", PATTERN_CFG_FILE)
					)
					.add(
						builder
							.newFilter("filters", ACCEPT, DENY)
							.addComponent(
								builder
									.newFilter("MarkerFilter", ACCEPT, DENY)
									.addAttribute("marker", Markers.CFG.getName())
							)
							.addComponent(
								builder
									.newFilter("ThresholdFilter", ACCEPT, DENY)
									.addAttribute("level", Level.INFO)
							)
					)
			)
			.add(
				builder
					.newAppender("async", "Async")
					.addAttribute("blocking", false)
					.addAttribute("bufferSize", 100000)
					.addComponent(builder.newAppenderRef("stdout"))
					.addComponent(builder.newAppenderRef("WebUI"))
					.addComponent(builder.newAppenderRef("msgFile"))
					.addComponent(builder.newAppenderRef("errFile"))
					.addComponent(builder.newAppenderRef("3rdPartyFile"))
					.addComponent(builder.newAppenderRef("itemListFile"))
					.addComponent(builder.newAppenderRef("perfAvgFile"))
					.addComponent(builder.newAppenderRef("perfSumFile"))
					.addComponent(builder.newAppenderRef("perfTraceFile"))
					.addComponent(builder.newAppenderRef("cfgFile"))
			)
			.add(
				builder
					.newAsyncRootLogger(Level.DEBUG)
					.addAttribute("additivity", false)
					.add(builder.newAppenderRef("async"))
			)
			.add(
				builder
					.newAsyncLogger("com.emc.mongoose", Level.DEBUG)
					.addAttribute("additivity", false)
					.add(builder.newAppenderRef("async"))
			)
			.build();
	}
	//
	@Override
	public final Configuration getConfiguration(final ConfigurationSource source) {
		return getConfiguration(source.toString(), null);
	}
	//
	@Override
	public final Configuration getConfiguration(final String name, final URI configLocation) {
		final ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
		return createConfiguration(name, builder);
	}
	//
	@Override
	protected final String[] getSupportedTypes() {
		return new String[] {"*"};
	}
	//
}
