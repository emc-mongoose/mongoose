package com.emc.mongoose.common.log.appenders;
//

import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.util.CircularArray;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.emc.mongoose.common.config.Constants.KEY_RUN_ID;

/**
 Created by kurila on 23.10.14.
 */
@Plugin(name="WebUI", category="Core", elementType="appender", printObject=true)
public final class WebUIAppender
extends AbstractAppender {

	private final static int MAX_EVENTS_IN_THE_LIST = 3000;
	//
	private static final List<String> markerNames = Collections.unmodifiableList(Arrays.asList(
		Markers.MSG.getName(), Markers.ERR.getName(),
			Markers.PERF_AVG.getName(), Markers.PERF_SUM.getName()));
	//
	public static final Map<String, Map<String, CircularArray<ShortenedLogEvent>>>
		LOG_EVENTS_MAP = new ConcurrentHashMap<>();
	//
	private static final Layout<? extends Serializable>
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
				if(!LOG_EVENTS_MAP.containsKey(currRunId)) {
					final Map<String, CircularArray<ShortenedLogEvent>> markers =
						new ConcurrentHashMap<>();
					for (final String markerName: markerNames) {
						addMarkerToMap(markers, markerName);
					}
					LOG_EVENTS_MAP.put(
						currRunId, markers
					);
				}
				final String eventMarkerName = event.getMarker().getName();
				if (markerNames.contains(eventMarkerName)) {
					addLogEventToMap(currRunId, eventMarkerName, event);
				}
			} // else silently skip
		}
	}
	//
	private void addMarkerToMap(final Map<String, CircularArray<ShortenedLogEvent>> markers,
	                            final String markerName) {
		markers.put(markerName,
				new CircularArray<>(MAX_EVENTS_IN_THE_LIST, new ShortenedLogEvent.SleComparator()));
	}
	//
	private void addLogEventToMap(final String runId, final String markerName,
	                              final LogEvent event) {
		LOG_EVENTS_MAP.get(runId).get(markerName).addItem(new ShortenedLogEvent(event));
	}
	//
	public static  Map<String, List<ShortenedLogEvent>> getLastLogEventsByMarker(
			final String runId,
			final String markerName,
			final long timeStamp) {
		final Map<String, List<ShortenedLogEvent>> lastLogEventsWithMarker =
				new ConcurrentHashMap<>();
		final Map<String, CircularArray<ShortenedLogEvent>> testLogs = LOG_EVENTS_MAP.get(runId);
		List<ShortenedLogEvent> lastLogEventsForMarker = null;
		if (testLogs != null && testLogs.containsKey(markerName)) {
			lastLogEventsForMarker = testLogs.get(markerName).getLastItems(new ShortenedLogEvent
					(timeStamp));
		}
		if (lastLogEventsForMarker != null) {
			lastLogEventsWithMarker.put(markerName, lastLogEventsForMarker);
		}
		return lastLogEventsWithMarker;
	}
	//
	public static void removeRunId(final String runId) {
		if (ENABLED_FLAG) {
			LOG_EVENTS_MAP.remove(runId);
		}
	}
}
