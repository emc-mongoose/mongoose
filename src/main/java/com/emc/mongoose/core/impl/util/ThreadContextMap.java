package com.emc.mongoose.core.impl.util;
//
import com.emc.mongoose.core.impl.util.RunTimeConfig;
//
import org.apache.logging.log4j.ThreadContext;
/**
 * Created by gusakk on 11/13/14.
 */
public final class ThreadContextMap {
	//
	public static void initThreadContextMap() {
		if (RunTimeConfig.getContext() != null) {
			final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
			ThreadContext.put(RunTimeConfig.KEY_RUN_ID, localRunTimeConfig.getRunId());
			ThreadContext.put(RunTimeConfig.KEY_RUN_MODE, localRunTimeConfig.getRunMode());
		} else {
			ThreadContext.put(RunTimeConfig.KEY_RUN_ID, System.getProperty(RunTimeConfig.KEY_RUN_ID));
			ThreadContext.put(RunTimeConfig.KEY_RUN_MODE, System.getProperty(RunTimeConfig.KEY_RUN_MODE));
		}
	}
	//
	public static void putValue(final String key, final String value) {
		ThreadContext.put(key, value);
	}
	//
}
