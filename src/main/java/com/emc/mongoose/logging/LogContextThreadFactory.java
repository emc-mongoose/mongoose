package com.emc.mongoose.logging;

import com.emc.mongoose.exception.InterruptRunException;

import com.github.akurilov.commons.concurrent.ContextAwareThreadFactory;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 Created by kurila on 19.07.16.
 */
public final class LogContextThreadFactory
extends ContextAwareThreadFactory {

	private static final Logger LOG = Logger.getLogger(LogContextThreadFactory.class.getName());

	public LogContextThreadFactory(final String threadNamePrefix) {
		super(threadNamePrefix, ThreadContext.getContext());
	}

	public LogContextThreadFactory(final String threadNamePrefix, final boolean daemonFlag) {
		super(threadNamePrefix, daemonFlag, ThreadContext.getContext());
	}

	private static final class LogContextThread
	extends ContextAwareThread {

		private LogContextThread(
			final Runnable task, final String name, final boolean daemonFlag,
			final UncaughtExceptionHandler exceptionHandler, final Map<String, String> threadContext
		) {
			super(task, name, daemonFlag, null, threadContext);
		}

		@Override
		public final void run() {
			try(final Instance ctx = CloseableThreadContext.putAll(threadContext)) {
				super.run();
			} catch(final InterruptRunException e) {
				throw e;
			} catch(final Throwable cause) {
				LOG.log(Level.SEVERE, "Unhandled thread failure", cause);
			}
		}
	}

	@Override
	public final Thread newThread(final Runnable task) {
		return new LogContextThread(
			task, threadNamePrefix + "#" + threadNumber.incrementAndGet(), daemonFlag,
			exceptionHandler, threadContext
		);
	}
}
