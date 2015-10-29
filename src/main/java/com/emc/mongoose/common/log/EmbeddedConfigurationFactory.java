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
//
import java.net.URI;
/**
 Created by kurila on 29.10.15.
 */
@Plugin(name = "EmbeddedConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class EmbeddedConfigurationFactory
extends ConfigurationFactory {
	//
	public static String
		STDOUT_PATTERN = "%highlight{%d{ISO8601}{GMT+0} %p{length=1} %-20.-20c{1} %-40.-40t %m%n}";
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
							.addAttribute("pattern", STDOUT_PATTERN)
					)
					.add(
						builder
							.newFilter("filters", NEUTRAL, NEUTRAL)
							.addComponent(
								builder
									.newFilter(
										"MarkerFilter", DENY, NEUTRAL
									)
									.addAttribute("marker", Markers.CFG.getName())
							)
							.addComponent(
								builder
									.newFilter(
										"MarkerFilter", DENY, NEUTRAL
									)
									.addAttribute("marker", Markers.ITEM_LIST.getName())
							)
							.addComponent(
								builder
									.newFilter(
										"MarkerFilter", DENY, NEUTRAL
									)
									.addAttribute("marker", Markers.PERF_TRACE.getName())
							)
							.addComponent(
								builder
									.newFilter(
										"ThresholdFilter", ACCEPT, DENY
									)
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
