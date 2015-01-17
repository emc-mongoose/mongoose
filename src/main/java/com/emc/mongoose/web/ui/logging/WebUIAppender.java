package com.emc.mongoose.web.ui.logging;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.web.ui.websockets.WebSocketLogListener;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.web.ui.websockets.WebSocketLogListener;
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
import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 Created by kurila on 23.10.14.
 */
@Plugin(name="WebUI", category="Core", elementType="appender", printObject=true)
public final class WebUIAppender
extends AbstractAppender {
	private final static int MAX_ELEMENTS_IN_THE_LIST = 10000;
	//
	private final static ConcurrentHashMap<String, CircularFifoQueue<LogEvent>>
		LOG_EVENTS_MAP = new ConcurrentHashMap<>();
	private final static List<WebSocketLogListener>
		LISTENERS = Collections.synchronizedList(new LinkedList<WebSocketLogListener>());
	//
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
	}
	//
	@PluginFactory
	public static WebUIAppender createAppender(
		final @PluginAttribute("name") String name,
		final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
		final @PluginAttribute("enabled") Boolean enabled,
		final @PluginElement("Filters") Filter filter
	) {
		if(name == null) {
			LOGGER.error("No name provided for CustomAppender");
			return null;
		}
		ENABLED_FLAG = enabled;
		return new WebUIAppender(name, filter, DEFAULT_LAYOUT, ignoreExceptions);
	}
	//
	public static void register(final WebSocketLogListener listener) {
		if(ENABLED_FLAG) {
			sendPreviousLogs(listener);
			LISTENERS.add(listener);
		}
	}
	//
	public static void unregister(final WebSocketLogListener listener) {
		if(ENABLED_FLAG) {
			LISTENERS.remove(listener);
		}
	}
	//
	public synchronized static void sendPreviousLogs(final WebSocketLogListener listener) {
		for (final CircularFifoQueue<LogEvent> queue : LOG_EVENTS_MAP.values()) {
			for (final LogEvent logEvent : queue) {
				listener.sendMessage(logEvent);
			}
		}
	}
	//
	@Override
	public synchronized final void append(final LogEvent event) {
		if (ENABLED_FLAG) {
			final String currentRunId = event.getContextMap().get(Main.KEY_RUN_ID);
			if (LOG_EVENTS_MAP.get(currentRunId) == null) {
				LOG_EVENTS_MAP.put(currentRunId, new CircularFifoQueue<LogEvent>(MAX_ELEMENTS_IN_THE_LIST));
			}
			LOG_EVENTS_MAP.get(currentRunId).add(event);
			for (final WebSocketLogListener listener : LISTENERS) {
				listener.sendMessage(event);
			}
		}
	}
	//
	public static void removeRunId(final String runId) {
		LOG_EVENTS_MAP.remove(runId);
	}
}
