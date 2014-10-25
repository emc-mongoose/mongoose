package com.emc.mongoose.util.logging;
//
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
//
import java.io.Serializable;
/**
 Created by kurila on 23.10.14.
 */
@Plugin(name="Custom", category="Core", elementType="appender", printObject=true)
public final class CustomAppender
extends AbstractAppender {
	//
	private final static Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	//
	private CustomAppender(
		final String name, final Filter filter, final Layout<? extends Serializable> layout
	) {
		super(name, filter, layout);
	}
	//
	@PluginFactory
	public static CustomAppender createAppender(
		final @PluginAttribute("name") String name,
		final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
		final @PluginElement("Filters") Filter filter
	) {
		if (name == null) {
			LOGGER.error("No name provided for CustomAppender");
			return null;
		}
		return new CustomAppender(name, filter, DEFAULT_LAYOUT);
	}
	//
	@Override
	public final void append(final LogEvent event) {
		System.out.println("Invoked custom log event: \"" + event.toString() + "\"");
	}
}
