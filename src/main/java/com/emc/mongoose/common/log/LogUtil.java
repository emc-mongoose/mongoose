package com.emc.mongoose.common.log;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configurator;
//
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 06.05.14.
 */
public final class LogUtil {
	private final static String
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
		FNAME_LOG_CONF = "logging.json",
		//
		MONGOOSE = "mongoose";
	//
	public static final Lock HOOKS_LOCK = new ReentrantLock();
	public static final Condition HOOKS_COND = HOOKS_LOCK.newCondition();
	public static final AtomicInteger LOAD_HOOKS_COUNT = new AtomicInteger(0);
	//
	public static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
	public static final Locale LOCALE_DEFAULT = Locale.ROOT;
	public static final DateFormat FMT_DT = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS",LOCALE_DEFAULT) {
		{ setTimeZone(TZ_UTC); }
	};
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
		WHITE = "\u001B[37m",
		//
		PATH_LOG_DIR = String.format("%s%slog", RunTimeConfig.DIR_ROOT, File.separator);
	//
	private static LoggerContext LOG_CTX = null;
	private final static Lock LOG_CTX_LOCK = new ReentrantLock();
	static {
		init();
	}
	public static void init() {
		LOG_CTX_LOCK.lock();
		try {
			if(LOG_CTX == null) {
				System.setProperty(KEY_THREAD_CTX_INHERIT, VALUE_THREAD_CTX_INHERIT);
				// make all used loggers asynchronous
				System.setProperty(KEY_LOG4J_CTX_SELECTOR, VALUE_LOG4J_CTX_ASYNC_SELECTOR);
				// connect JUL to Log4J2
				System.setProperty(KEY_JUL_MANAGER, VALUE_JUL_MANAGER);
				// set "run.id" property with timestamp value if not set before
				String runId = System.getProperty(RunTimeConfig.KEY_RUN_ID);
				if(runId == null || runId.length() == 0) {
					System.setProperty(
						RunTimeConfig.KEY_RUN_ID,
						FMT_DT.format(
							Calendar.getInstance(TZ_UTC, LOCALE_DEFAULT).getTime()
						)
					);
				}
				// determine the logger configuration file path
				Path logConfPath = Paths.get(
					RunTimeConfig.DIR_ROOT, Constants.DIR_CONF, FNAME_LOG_CONF
				);
				//
				try {
					if(Files.exists(logConfPath)) {
						LOG_CTX = Configurator.initialize(MONGOOSE, logConfPath.toUri().toString());
					} else if(System.getProperty("log4j.configurationFile") == null) {
						final ClassLoader classloader = LogUtil.class.getClassLoader();
						final URL bundleLogConfURL = classloader.getResource(FNAME_LOG_CONF);
						if(bundleLogConfURL != null) {
							LOG_CTX = Configurator.initialize(MONGOOSE, classloader, bundleLogConfURL.toURI());
						}
					} else {
						LOG_CTX = Configurator.initialize(MONGOOSE, System.getProperty("log4j.configurationFile"));
					}
					//
					if(LOG_CTX == null) {
						System.err.println("Logging configuration failed");
					} else {
						LogManager.getLogger().info(
							Markers.MSG, "Logging subsystem is configured successfully"
						);
						Runtime.getRuntime().addShutdownHook(
							new Thread("logCtxShutDownHook") {
								@Override
								public final void run() {
									shutdown();
								}
							}
						);
					}
					System.setErr(
						new PrintStream(
							new StdErrLoggingStream(LogManager.getRootLogger(), Markers.ERR), true
						)
					);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		} finally {
			LOG_CTX_LOCK.unlock();
		}
	}
	//
	public static void shutdown() {
		final Logger LOG = LogManager.getLogger();
		try {
			if(LOAD_HOOKS_COUNT.get() != 0) {
				LOG.debug(Markers.MSG, "Not all loads are closed, blocking the logging subsystem shutdown");
				if(HOOKS_LOCK.tryLock(10, TimeUnit.SECONDS)) {
					try {
						if(HOOKS_COND.await(10, TimeUnit.SECONDS)) {
							LOG.debug(Markers.MSG, "All load executors are closed");
						} else {
							LOG.debug(Markers.ERR, "Timeout while waiting the load executors to be closed");
						}
					} finally {
						HOOKS_LOCK.unlock();
					}
				} else {
					LOG.debug(Markers.ERR, "Failed to acquire the lock for the del method");
				}
			} else {
				LOG.debug(Markers.MSG, "There's no unclosed loads, forcing logging subsystem shutdown");
			}
		} catch (final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Shutdown method was interrupted");
		} finally {
			LOG_CTX_LOCK.lock();
			try {
				if(LOG_CTX != null && !LOG_CTX.isStopped()) {
					LOG_CTX.stop();
				}
			} finally {
				LOG_CTX_LOCK.unlock();
			}
		}
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
}
