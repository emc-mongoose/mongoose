package com.emc.mongoose.common.concurrent;

import java.util.concurrent.ForkJoinPool;

/**
 Created by kurila on 09.09.15.
 */
public class ThreadUtil {
	public static int getHardwareThreadCount() {
		return ForkJoinPool.commonPool().getParallelism();
	}
}
