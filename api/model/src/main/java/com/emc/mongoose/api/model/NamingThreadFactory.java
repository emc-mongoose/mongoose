package com.emc.mongoose.api.model;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.ThreadContext;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 19.07.16.
 */
public class NamingThreadFactory
implements ThreadFactory {

	protected static final UncaughtExceptionHandler exceptionHandler = (t, e) -> {
		synchronized(System.err) {
			System.err.println("Uncaught exception in the thread \"" + t.getName() + "\":");
			e.printStackTrace(System.err);
		}
	};

	protected final AtomicInteger threadNumber = new AtomicInteger(0);
	protected final String threadNamePrefix;
	protected final boolean daemonFlag;
	protected final Map<String, String> context;

	public NamingThreadFactory(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
		this.daemonFlag = false;
		this.context = ThreadContext.getContext();
	}

	public NamingThreadFactory(
		final String threadNamePrefix, final boolean daemonFlag
	) {
		this.threadNamePrefix = threadNamePrefix;
		this.daemonFlag = daemonFlag;
		this.context = ThreadContext.getContext();
	}

	private static final class ContextAwareThread
	extends Thread {

		private final Map<String, String> context;

		private ContextAwareThread(
			final Runnable task, final String name, final Map<String, String> context
		) {
			super(task, name);
			this.context = context;
		}

		@Override
		public final void run() {
			try(final Instance ctx = CloseableThreadContext.putAll(context)) {
				super.run();
			}
		}
	}

	@Override
	public Thread newThread(final Runnable task) {
		final Thread t = new ContextAwareThread(
			task, threadNamePrefix + "#" + threadNumber.incrementAndGet(), context
		);
		t.setDaemon(daemonFlag);
		t.setUncaughtExceptionHandler(exceptionHandler);
		return t;
	}

	@Override
	public final String toString() {
		return threadNamePrefix;
	}
}
