package com.emc.mongoose.api.common.concurrent;

/**
 Created by kurila on 09.09.15.
 */
public class ThreadUtil {
	public static int getHardwareThreadCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
