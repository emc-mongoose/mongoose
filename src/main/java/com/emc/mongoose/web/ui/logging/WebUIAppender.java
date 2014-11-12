package com.emc.mongoose.web.ui.logging;
//
import com.emc.mongoose.web.ui.websockets.interfaces.WebSocketLogListener;
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
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 Created by kurila on 23.10.14.
 */
@Plugin(name="WebUI", category="Core", elementType="appender", printObject=true)
public final class WebUIAppender
extends AbstractAppender {
	//
	private static List<WebSocketLogListener> listeners;
	//
	private static List<LogEvent> logEvents;
	//
	private final static Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	//
	private WebUIAppender(
			final String name, final Filter filter, final Layout<? extends Serializable> layout
	) {
		super(name, filter, layout);
		listeners = new CopyOnWriteArrayList<>();
		logEvents = new CopyOnWriteArrayList<>();
	}
	//
	@PluginFactory
	public static WebUIAppender createAppender(
		final @PluginAttribute("name") String name,
		final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
		final @PluginElement("Filters") Filter filter
	) {
		if (name == null) {
			LOGGER.error("No name provided for CustomAppender");
			return null;
		}
		return new WebUIAppender(name, filter, DEFAULT_LAYOUT);
	}
	//
	public static void register(final WebSocketLogListener listener) {
		listeners.add(listener);
	}

	public static void unregister(final WebSocketLogListener listener) {
		listeners.remove(listener);
	}

	public static List<LogEvent> getLogEventsList() {
		return logEvents;
	}
	//
	@Override
	public final void append(final LogEvent event) {
		logEvents.add(event);
		for (WebSocketLogListener listener : listeners) {
			listener.sendMessage(event);
		}
	}
}
