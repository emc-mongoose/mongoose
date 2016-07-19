package com.emc.mongoose.common.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 19.07.16.
 */
public class NamingThreadFactory
	implements ThreadFactory {
	//
	protected final AtomicInteger threadNumber = new AtomicInteger(0);
	protected final String threadNamePrefix;
	protected final boolean daemonFlag;
	//
	public NamingThreadFactory(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
		this.daemonFlag = false;
	}
	//
	public NamingThreadFactory(final String threadNamePrefix, final boolean daemonFlag) {
		this.threadNamePrefix = threadNamePrefix;
		this.daemonFlag = daemonFlag;
	}
	//
	@Override
	public Thread newThread(final Runnable task) {
		final Thread t = new Thread(task, threadNamePrefix + "#" + threadNumber.incrementAndGet());
		t.setDaemon(daemonFlag);
		return t;
	}
	//
	@Override
	public final String toString() {
		return threadNamePrefix;
	}
}
