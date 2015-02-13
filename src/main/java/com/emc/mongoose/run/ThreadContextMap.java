package com.emc.mongoose.run;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.logging.log4j.ThreadContext;
/**
 * Created by gusakk on 11/13/14.
 */
public final class ThreadContextMap {
	//
	public static void initThreadContextMap() {
		if (Main.RUN_TIME_CONFIG.get() != null) {
			final RunTimeConfig localRunTimeConfig = Main.RUN_TIME_CONFIG.get();
			ThreadContext.put(RunTimeConfig.KEY_RUN_ID, localRunTimeConfig.getRunId());
			ThreadContext.put(RunTimeConfig.KEY_RUN_MODE, localRunTimeConfig.getRunMode());
			ThreadContext.put(RunTimeConfig.KEY_RUN_TIMESTAMP, localRunTimeConfig.getRunTimestamp());
		} else {
			ThreadContext.put(RunTimeConfig.KEY_RUN_ID, System.getProperty(RunTimeConfig.KEY_RUN_ID));
			ThreadContext.put(RunTimeConfig.KEY_RUN_MODE, System.getProperty(RunTimeConfig.KEY_RUN_MODE));
			ThreadContext.put(RunTimeConfig.KEY_RUN_TIMESTAMP, System.getProperty(RunTimeConfig.KEY_RUN_TIMESTAMP));
		}
	}
	//
	public static void putValue(final String key, final String value) {
		ThreadContext.put(key, value);
	}
	//
}
