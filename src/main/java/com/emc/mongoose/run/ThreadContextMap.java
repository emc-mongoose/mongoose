package com.emc.mongoose.run;

import com.emc.mongoose.util.conf.RunTimeConfig;
import org.apache.logging.log4j.ThreadContext;

/**
 * Created by gusakk on 11/13/14.
 */
public class ThreadContextMap {

	public static void initThreadContextMap(final RunTimeConfig runTimeConfig) {
		ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
	}

}
