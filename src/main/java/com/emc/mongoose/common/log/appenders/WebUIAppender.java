package com.emc.mongoose.common.log.appenders;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.processors.VSimplifier;
//
import com.emc.mongoose.common.log.appenders.processors.VSimplifierLogAdapter;
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
	private final static int MAX_ELEMENTS_IN_THE_LIST = 10000;
	private final static int SIMPLIFICATION_LIMIT = 4;
	private final static int NUMBER_OF_SIMPLIFICATIONS = 1;
	private final static String NON_SIMPLIFIABLE_EVENTS_KEY = "other";
	//
	private final static ConcurrentHashMap<String, Map<String, List<LogEvent>>>
			LOG_EVENTS_MAP = new ConcurrentHashMap<>();
	private final static List<WebSocketLogListener>
			LISTENERS = Collections.synchronizedList(new LinkedList<WebSocketLogListener>());
	//
	private final static Layout<? extends Serializable>
			DEFAULT_LAYOUT = SerializedLayout.createLayout();
	//
	private final String KEY_RUN_ID = RunTimeConfig.KEY_RUN_ID;
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
		final List<LogEvent> previousLogs = new ArrayList<>();
		for (final Map<String, List<LogEvent>> map: LOG_EVENTS_MAP.values()) {
			for (List<LogEvent> list: map.values()) {
				for (final LogEvent logEvent : list) {
					previousLogs.add(logEvent);
				}
			}
		}
		listener.sendMessage(previousLogs);
	}
	//
	@Override
	public synchronized final void append(final LogEvent event) {
		if(ENABLED_FLAG) {
			final String currRunId;
			final Map<String, String> evtCtxMap = event.getContextMap();
			if(evtCtxMap.containsKey(KEY_RUN_ID)) {
				currRunId = evtCtxMap.get(KEY_RUN_ID);
			} else {
				currRunId = ThreadContext.get(KEY_RUN_ID);
			}
			//
			if(currRunId != null) {
				Map<String, List<LogEvent>> loadJobMap;
				if (!LOG_EVENTS_MAP.containsKey(currRunId)) {
					loadJobMap = new HashMap<>();
					loadJobMap.put(NON_SIMPLIFIABLE_EVENTS_KEY, new ArrayList<LogEvent>());
					LOG_EVENTS_MAP.put(currRunId, loadJobMap);
				}
				loadJobMap = LOG_EVENTS_MAP.get(currRunId);
				if (event.getMarker().equals(Markers.PERF_AVG)) {
					String loadJobName = evtCtxMap.get(LogUtil.LOAD_JOB_NAME);
					if (!loadJobMap.containsKey(loadJobName)) {
						loadJobMap.put(loadJobName, new ArrayList<LogEvent>());
					}
					List<LogEvent> perfAvgEvents = loadJobMap.get(loadJobName);
					perfAvgEvents.add(event);
					if (perfAvgEvents.size() >= SIMPLIFICATION_LIMIT) {
						loadJobMap.put(loadJobName, removeExtraLogEvents(perfAvgEvents));
					}
				} else {
					loadJobMap.get(NON_SIMPLIFIABLE_EVENTS_KEY).add(event);
				}
				for (final WebSocketLogListener listener : LISTENERS) {
					listener.sendMessage(event);
				}
			} // else silently skip
		}
	}

	private List<LogEvent> removeExtraLogEvents(List<LogEvent> events) {
		VSimplifierLogAdapter simplifier = new VSimplifierLogAdapter(events);
		return simplifier.simplify(NUMBER_OF_SIMPLIFICATIONS);
	}

	//
	public static void removeRunId(final String runId) {
		if (ENABLED_FLAG) {
			LOG_EVENTS_MAP.remove(runId);
		}
	}
}
