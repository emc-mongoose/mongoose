package com.emc.mongoose.common.concurrent;
/**
 Created by kurila on 09.09.15.
 */
public class ThreadUtil {
	public static int getWorkerCount() {
		return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
	}
}
