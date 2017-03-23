package com.emc.mongoose.ui.log;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.env.PathUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.datetime.DatePrinter;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.emc.mongoose.common.Constants.DIR_CONFIG;
import static com.emc.mongoose.common.Constants.FNAME_LOG_CONFIG;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.Constants.LOCALE_DEFAULT;
import static com.emc.mongoose.common.env.DateUtil.TZ_UTC;

/**
 Created by kurila on 06.05.14.
 */
public final class LogUtil
implements ShutdownCallbackRegistry {
	//
	private static final String
		//
		KEY_LOG4J_CTX_SELECTOR = "Log4jContextSelector",
		VALUE_LOG4J_CTX_ASYNC_SELECTOR = AsyncLoggerContextSelector.class.getCanonicalName(),
		//
		KEY_JUL_MANAGER = "java.util.logging.manager",
		VALUE_JUL_MANAGER = "org.apache.logging.log4j.jul.LogManager",
		//
		KEY_THREAD_CTX_INHERIT = "isThreadContextMapInheritable",
		VALUE_THREAD_CTX_INHERIT = Boolean.toString(true),
		//
		KEY_WAIT_STRATEGY = "AsyncLogger.WaitStrategy",
		VALUE_WAIT_STRATEGY = "Block",
		//
		KEY_CLOCK = "log4j.Clock",
		VALUE_CLOCK = "CoarseCachedClock",
		//
		KEY_SHUTDOWN_CALLBACK_REGISTRY = "log4j.shutdownCallbackRegistry",
		VALUE_SHUTDOWN_CALLBACK_REGISTRY = "com.djdch.log4j.StaticShutdownCallbackRegistry",
		//
		KEY_CONFIG_FACTORY = "log4j.configurationFactory",
		VALUE_CONFIG_FACTORY = "org.apache.logging.log4j.core.config.json.JsonConfigurationFactory",
		//
		FNAME_LOG_CONF = "conf/logging.json",
		//
		MONGOOSE = "mongoose";
	//
	public static final DatePrinter
		FMT_DT = FastDateFormat.getInstance("yyyy.MM.dd.HH.mm.ss.SSS", TZ_UTC, LOCALE_DEFAULT);
	// console colors
	public static final String
		RESET = "\u001B[0m",
		BLACK = "\u001B[30m",
		RED = "\u001B[31m",
		GREEN = "\u001B[32m",
		INT_RED_OVER_GREEN = RED + "%d" + GREEN,
		YELLOW = "\u001B[33m",
		//
		INT_YELLOW_OVER_GREEN = YELLOW + "%d" + GREEN,
		BLUE = "\u001B[34m",
		PURPLE = "\u001B[35m",
		CYAN = "\u001B[36m",
		WHITE = "\u001B[37m";
	//
	private static LoggerContext LOG_CTX = null;
	private static volatile boolean STDOUT_COLORING_ENABLED = false;
	private static final Lock LOG_CTX_LOCK = new ReentrantLock();
	
	//
	public static String getDateTimeStamp() {
		return FMT_DT.format(
			Calendar.getInstance(TZ_UTC, LOCALE_DEFAULT).getTime()
		);
	}
	//
	private static boolean isStdOutColoringEnabledByConfig() {
		if(LOG_CTX != null) {
			final Appender consoleAppender = LOG_CTX.getConfiguration().getAppender("stdout");
			if(consoleAppender != null) {
				final Layout consoleAppenderLayout = consoleAppender.getLayout();
				if(consoleAppenderLayout instanceof PatternLayout) {
					final String pattern = ((PatternLayout) consoleAppenderLayout)
						.getConversionPattern();
					if(pattern != null && pattern.contains("%highlight")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	//
	public static void init() {
		LOG_CTX_LOCK.lock();
		try {
			if(LOG_CTX == null) {
				System.setProperty(KEY_THREAD_CTX_INHERIT, VALUE_THREAD_CTX_INHERIT);
				// make all used loggers asynchronous
				System.setProperty(KEY_LOG4J_CTX_SELECTOR, VALUE_LOG4J_CTX_ASYNC_SELECTOR);
				// connect JUL to Log4J2
				System.setProperty(KEY_JUL_MANAGER, VALUE_JUL_MANAGER);
				System.setProperty(KEY_WAIT_STRATEGY, VALUE_WAIT_STRATEGY);
				System.setProperty(KEY_CLOCK, VALUE_CLOCK);
				System.setProperty(KEY_SHUTDOWN_CALLBACK_REGISTRY, LogUtil.class.getCanonicalName());
				System.setProperty(KEY_CONFIG_FACTORY, VALUE_CONFIG_FACTORY);
				// set "load.job.name" property with timestamp value if not set before
				final String loadJobName = ThreadContext.get(KEY_STEP_NAME);
				if(loadJobName == null || loadJobName.length() == 0) {
					ThreadContext.put(KEY_STEP_NAME, getDateTimeStamp());
				}
				try {
					String log4jConfigFile = System.getProperty("log4j.configurationFile");
					if(log4jConfigFile == null) {
						log4jConfigFile = PathUtil.getBaseDir() + File.separator + DIR_CONFIG +
							File.separator + FNAME_LOG_CONFIG;
					}
					LOG_CTX = Configurator.initialize(MONGOOSE, log4jConfigFile);
					//
					if(LOG_CTX == null) {
						System.err.println("Logging configuration failed");
					} else {
						Runtime.getRuntime().addShutdownHook(
							new Thread("logCtxShutDownHook") {
								@Override
								public final void run() {
									shutdown();
								}
							}
						);
					}
					/*final IoBuilder logStreamBuilder = IoBuilder.forLogger(DriverManager.class);
					System.setErr(
						logStreamBuilder
							.setLevel(Level.DEBUG)
							.setMarker(Markers.ERR)
							.setAutoFlush(true)
							.setBuffered(true)
							.buildPrintStream()
					);*/
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		} finally {
			STDOUT_COLORING_ENABLED = isStdOutColoringEnabledByConfig();
			LOG_CTX_LOCK.unlock();
		}
	}
	//
	public static void shutdown() {
		Daemon.closeAll();
		// stop the logging
		LOG_CTX_LOCK.lock();
		try {
			if(LOG_CTX != null) {
				if(LOG_CTX.isStarted()) {
					LOG_CTX.stop();
				}
				LOG_CTX = null;
			}
		} finally {
			LOG_CTX_LOCK.unlock();
		}
	}
	//
	public static boolean isConsoleColoringEnabled() {
		return STDOUT_COLORING_ENABLED;
	}
	//
	public static void exception(
		final Logger logger, final Level level, final Throwable e,
		final String msgPattern, final Object... args
	) {
		if(logger.isTraceEnabled(Markers.ERR)) {
			logger.log(
				level, Markers.ERR,
				logger.getMessageFactory().newMessage(msgPattern + ": " + e, args), e
			);
		} else {
			logger.log(
				level, Markers.ERR,
				logger.getMessageFactory().newMessage(msgPattern + ": " + e, args)
			);
		}
	}
	//
	public static void trace(
		final Logger logger, final Level level, final Marker marker,
		final String msgPattern, final Object... args
	) {
		logger.log(
			level, marker, logger.getMessageFactory().newMessage(msgPattern, args), new Throwable()
		);
	}

	@Override
	public final Cancellable addShutdownCallback(final Runnable callback) {
		return new Cancellable() {
			@Override
			public final void cancel() {
			}
			@Override
			public final void run() {
				if(callback != null) {
					callback.run();
				}
			}
		};
	}
}
