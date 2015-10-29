package com.emc.mongoose.common.log;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
/**
 Created by kurila on 29.10.15.
 */
public class EmbeddedConfigurationFactory
extends ConfigurationBuilderFactory {
	//
	public static Configuration createConfiguration(
		final String name, final ConfigurationBuilder<BuiltConfiguration> builder
	) {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		//
		builder.setConfigurationName(rtConfig.getRunName());
		builder.setStatusLevel(Level.DEBUG);
		builder.setPackages(EmbeddedConfigurationFactory.class.getPackage().getName() + ".appenders");
		builder.setShutdownHook("enable");
		// appenders
		return builder
			.add(
				builder
					.newAppender("stdout", "Console")
					.addAttribute("follow", true)
					.add(
						builder
							.newLayout("PatternLayout")
							.addAttribute("alwaysWriteExceptions", false)
							.addAttribute(
								"pattern",
								"%highlight{%d{ISO8601}{GMT+0} %p{length=1} %-20.-20c{1} %-40.-40t %m%n}"
							)
					)
					.add(
						builder
							.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
							.addAttribute("marker", Markers.CFG)
					)
					.add(
						builder
							.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
							.addAttribute("marker", Markers.ITEM_LIST)
					)
					.add(
						builder
							.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
							.addAttribute("marker", Markers.PERF_TRACE)
					)
					.add(
						builder
							.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
							.addAttribute("level", Level.INFO)
					)
			)
			.add(
				builder
					.newAppender("async", "Async")
					.addAttribute("blocking", false)
					.addAttribute("bufferSize", 100000)
					.addComponent(builder.newAppenderRef("stdout"))
			)
			.add(
				builder
					.newAsyncRootLogger(Level.DEBUG)
					.addAttribute("additivity", false)
					.add(builder.newAppenderRef("async"))
			)
			.build();
	}
	//
	//
}
