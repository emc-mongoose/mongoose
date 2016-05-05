package com.emc.mongoose.common.log.appenders;
//
import com.emc.mongoose.common.conf.AppConfig;
//
//
import com.emc.mongoose.common.log.Markers;
import org.apache.commons.collections4.queue.CircularFifoQueue;
//
import org.apache.logging.log4j.ThreadContext;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 23.10.14.
 */
@Plugin(name="WebUI", category="Core", elementType="appender", printObject=true)
public final class WebUIAppender
extends AbstractAppender {

	private final static int MAX_EVENTS_IN_THE_LIST = 3000;
	//
	private static final String MESSAGE_MARKER_KEY = "messages";
	private static final String ERRORS_MARKER_KEY = "errors";
	private static final String PERF_AVG_MARKER_KEY = "perf.avg";
	private static final String PERF_SUM_MARKER_KEY = "perf.sum";
	//
	private static final String LEVEL_EVENT_KEY = "level";
	private static final String LOGGER_NAME_EVENT_KEY = "loggerName";
	private static final String THREAD_NAME_EVENT_KEY = "threadName";
	private static final String TIME_EVENT_KEY = "timeMillis";
	private static final String MESSAGE_EVENT_KEY = "message";
	//
	private static final ConcurrentHashMap<String, Map<String, Queue<String>>>
		LOG_EVENTS_MAP = new ConcurrentHashMap<>();
	//
	private static final Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	//
	private static boolean ENABLED_FLAG = true;
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
	@Override
	public synchronized final void append(final LogEvent event) {
		if(ENABLED_FLAG) {
			final String currRunId;
			final Map<String, String> evtCtxMap = event.getContextMap();
			if(evtCtxMap.containsKey(AppConfig.KEY_RUN_ID)) {
				currRunId = evtCtxMap.get(AppConfig.KEY_RUN_ID);
			} else {
				currRunId = ThreadContext.get(AppConfig.KEY_RUN_ID);
			}
			//
			if(currRunId != null) {
				if(!LOG_EVENTS_MAP.containsKey(currRunId)) {
					final Map<String, Queue<String>> markers = new ConcurrentHashMap<>();
					markers.put(MESSAGE_MARKER_KEY, new CircularFifoQueue<String>(MAX_EVENTS_IN_THE_LIST));
					markers.put(ERRORS_MARKER_KEY, new CircularFifoQueue<String>(MAX_EVENTS_IN_THE_LIST));
					markers.put(PERF_AVG_MARKER_KEY, new CircularFifoQueue<String>(MAX_EVENTS_IN_THE_LIST));
					markers.put(PERF_SUM_MARKER_KEY, new CircularFifoQueue<String>(MAX_EVENTS_IN_THE_LIST));
					LOG_EVENTS_MAP.put(
						currRunId, markers
					);
				}
//				switch (event.getMarker()) {
//					case Markers.MSG:
//						break;
//				}
//				LOG_EVENTS_MAP.get(currRunId).add(event);
			} // else silently skip
		}
	}
	//
	public static void removeRunId(final String runId) {
		if (ENABLED_FLAG) {
			LOG_EVENTS_MAP.remove(runId);
		}
	}
}
