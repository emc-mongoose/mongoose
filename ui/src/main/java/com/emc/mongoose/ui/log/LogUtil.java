package com.emc.mongoose.ui.log;

import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.DaemonBase;
import static com.emc.mongoose.common.Constants.DIR_CONFIG;
import static com.emc.mongoose.common.Constants.FNAME_LOG_CONFIG;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.Constants.LOCALE_DEFAULT;
import static com.emc.mongoose.common.env.DateUtil.TZ_UTC;

import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.json.JsonConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.datetime.DatePrinter;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.jul.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 06.05.14.
 */
public final class LogUtil
implements ShutdownCallbackRegistry {
	
	private static final Map<String, String> LOGGING_PROPS = new HashMap<String, String>() {
		{
			put("Log4jContextSelector", AsyncLoggerContextSelector.class.getCanonicalName());
			put("java.util.logging.manager", LogManager.class.getCanonicalName());
			put("isThreadContextMapInheritable", Boolean.toString(true));
			put("AsyncLogger.WaitStrategy", "Block");
			put("log4j.Clock", "CoarseCachedClock");
			put("log4j.shutdownCallbackRegistry", LogUtil.class.getCanonicalName());
			put("log4j.configurationFactory", JsonConfigurationFactory.class.getCanonicalName());
			put("log4j2.garbagefree.threadContextMap", Boolean.toString(true));
			put("log4j2.enable.threadlocals", Boolean.toString(true));
			put("log4j2.enable.direct.encoders", Boolean.toString(true));
		}
	};
	//
	private static final String NAME = "mongoose";
	//
	public static final DatePrinter
		FMT_DT = FastDateFormat.getInstance("yyyy.MM.dd.HH.mm.ss.SSS", TZ_UTC, LOCALE_DEFAULT);
	// console colors
	public static final String
		RESET = "\u001B[0m",
		BLACK = "\u001B[30m",
		RED = "\u001B[31m",
		GREEN = "\u001B[32m",
		YELLOW = "\u001B[33m",
		//
		BLUE = "\u001B[34m",
		PURPLE = "\u001B[35m",
		CYAN = "\u001B[36m",
		WHITE = "\u001B[37;1m";
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
				System.getProperties().putAll(LOGGING_PROPS);
				// set step name property with timestamp value if not set before
				final String testStepName = ThreadContext.get(KEY_STEP_NAME);
				if(testStepName == null || testStepName.length() == 0) {
					ThreadContext.put(KEY_STEP_NAME, getDateTimeStamp());
				}
				try {
					String log4jConfigFile = System.getProperty("log4j.configurationFile");
					if(log4jConfigFile == null) {
						log4jConfigFile = PathUtil.getBaseDir() + File.separator + DIR_CONFIG +
							File.separator + FNAME_LOG_CONFIG;
					}
					LOG_CTX = Configurator.initialize(NAME, log4jConfigFile);
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
						Loggers.MSG.info(
							"Logging initialized using the configuration file: {}", log4jConfigFile
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
		try {
			System.out.println("close all daemons...");
			DaemonBase.closeAll();
			System.out.println("flush all loggers...");
			LoadJobLogFileManager.flushAll();
		} catch(final Throwable cause) {
			cause.printStackTrace(System.err);
		}
		// stop the logging
		LOG_CTX_LOCK.lock();
		try {
			if(LOG_CTX != null) {
				if(LOG_CTX.isStarted()) {
					System.out.println("stop the loggers...");
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
		final Level level, final Throwable e,
		final String msgPattern, final Object... args
	) {
		if(Loggers.ERR.isTraceEnabled()) {
			Loggers.ERR.log(
				level, Loggers.ERR.getMessageFactory().newMessage(msgPattern + ": " + e, args), e
			);
		} else {
			Loggers.ERR.log(
				level, Loggers.ERR.getMessageFactory().newMessage(msgPattern + ": " + e, args)
			);
		}
	}
	//
	public static void trace(
		final Logger logger, final Level level, final String msgPattern, final Object... args
	) {
		logger.log(level, logger.getMessageFactory().newMessage(msgPattern, args), new Throwable());
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
					System.out.println("Shutdown callback + \"" + callback + "\" run...");
					callback.run();
				}
			}
		};
	}
}
