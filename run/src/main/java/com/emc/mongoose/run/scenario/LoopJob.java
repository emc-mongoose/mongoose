package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 Created by andrey on 01.07.16.
 */
public class LoopJob
extends SequentialJob {

	private static final Logger LOG = LogManager.getLogger();

	private final long count;

	public LoopJob(final Config appConfig, final Map<String, Object> subTree)
	throws NumberFormatException, IllegalArgumentException {
		super(appConfig, subTree);
		final Object value = subTree.get(KEY_NODE_VALUE);
		if(value != null) {
			if(value instanceof Long) {
				count = (Long) value;
			} else if(value instanceof Integer) {
				count = (Integer) value;
			} else if(value instanceof Short) {
				count = (Short) value;
			} else if(value instanceof String) {
				count = Long.parseLong((String) value);
			} else {
				throw new IllegalArgumentException("Unexpected value: \"" + value + "\"");
			}
		} else {
			count = Long.MAX_VALUE;
		}
	}

	@Override
	public String toString() {
		return "loopJob" + (count == Long.MAX_VALUE ? "Infinite" : count) + "#" + hashCode();
	}

	@Override
	public final void run() {
		for(long i = 0; i < count; i ++) {
			LOG.info(Markers.MSG, "{}: starting step #{}", toString(), i);
			super.run();
		}
	}
}
