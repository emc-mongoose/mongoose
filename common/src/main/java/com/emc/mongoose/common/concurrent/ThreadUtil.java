package com.emc.mongoose.common.concurrent;
/**
 Created by kurila on 09.09.15.
 */
public class ThreadUtil {
	public static int getHardwareConcurrencyLevel() {
		return Runtime.getRuntime().availableProcessors();
	}
}
