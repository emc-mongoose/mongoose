package com.emc.mongoose.util.logging;
//
import com.emc.mongoose.web.ui.websockets.LogSocket;
import com.emc.mongoose.web.ui.websockets.WebSocketLogListener;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEventListener;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
//
import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

/**
 Created by kurila on 23.10.14.
 */
@Plugin(name="Custom", category="Core", elementType="appender", printObject=true)
public final class CustomAppender
extends AbstractAppender {
	//
	private static Set<WebSocketLogListener> listeners;
	//
	private final static Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	//
	private CustomAppender(
		final String name, final Filter filter, final Layout<? extends Serializable> layout
	) {
		super(name, filter, layout);
		listeners = new HashSet<>();
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
	public static void register(WebSocketLogListener listener) {
		listeners.add(listener);
	}

	public static void unregister(WebSocketLogListener listener) {
		listeners.remove(listener);
	}
	//
	@Override
	public final void append(final LogEvent event) {
		for (WebSocketLogListener listener : listeners) {
			listener.sendMessage(event);
		}
	}
}
