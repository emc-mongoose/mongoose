package com.emc.mongoose.web.ui.logging;
//
import com.emc.mongoose.web.ui.websockets.WebSocketLogListener;
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
	private static List<WebSocketLogListener> LIST_LISTENERS;
	private static List<LogEvent> LIST_EVENTS;
	private final static Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	//
	private static boolean ENABLED_FLAG;
	//
	private WebUIAppender(
		final String name, final Filter filter, final Layout<? extends Serializable> layout,
		final boolean ignoreExceptions
	) {
		super(name, filter, layout, ignoreExceptions);
		LIST_LISTENERS = new CopyOnWriteArrayList<>();
		LIST_EVENTS = new CopyOnWriteArrayList<>();
	}
	//
	@PluginFactory
	public static WebUIAppender createAppender(
		final @PluginAttribute("name") String name,
		final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
		final @PluginAttribute("enabled") Boolean enabled,
		final @PluginElement("Filters") Filter filter
	) {
		if (name == null) {
			LOGGER.error("No name provided for CustomAppender");
			return null;
		}
		ENABLED_FLAG = enabled;
		return new WebUIAppender(name, filter, DEFAULT_LAYOUT, ignoreExceptions);
	}
	//
	public static void register(final WebSocketLogListener listener) {
		if(ENABLED_FLAG) {
			LIST_LISTENERS.add(listener);
		}
	}
	//
	public static void unregister(final WebSocketLogListener listener) {
		if(ENABLED_FLAG) {
			LIST_LISTENERS.remove(listener);
		}
	}
	//
	public static List<LogEvent> getLogEventsList() {
		return LIST_EVENTS;
	}
	//
	@Override
	public final void append(final LogEvent event) {
		if(ENABLED_FLAG) {
			LIST_EVENTS.add(event);
			for(final WebSocketLogListener listener : LIST_LISTENERS) {
				listener.sendMessage(event);
			}
		}
	}
}
