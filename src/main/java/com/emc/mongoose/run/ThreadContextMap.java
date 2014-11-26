package com.emc.mongoose.run;

import com.emc.mongoose.util.conf.RunTimeConfig;
import org.apache.logging.log4j.ThreadContext;

/**
 * Created by gusakk on 11/13/14.
 */
public class ThreadContextMap {
	// TODO validate the variant below
	public static void initThreadContextMap() {
		if (Main.RUN_TIME_CONFIG.get() != null) {
			final RunTimeConfig localRunTimeConfig = Main.RUN_TIME_CONFIG.get();
			ThreadContext.put(Main.KEY_RUN_ID, localRunTimeConfig.getRunId());
			ThreadContext.put(Main.KEY_RUN_MODE, localRunTimeConfig.getRunMode());
		} else {
			ThreadContext.put(Main.KEY_RUN_ID, System.getProperty(Main.KEY_RUN_ID));
			ThreadContext.put(Main.KEY_RUN_MODE, System.getProperty(Main.KEY_RUN_MODE));
		}
	}
	//
	public static void putValue(final String key, final String value) {
		ThreadContext.put(key, value);
	}
	//
}
