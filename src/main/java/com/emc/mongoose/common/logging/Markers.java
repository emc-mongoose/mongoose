package com.emc.mongoose.common.logging;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
//
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configurator;
//
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 06.05.14.
 */
public final class Markers {
	//
	private final static String
		//
		KEY_LOG4J_CTX_SELECTOR = "Log4jContextSelector",
		VALUE_LOG4J_CTX_SELECTOR = AsyncLoggerContextSelector.class.getCanonicalName(),
		//
		KEY_JUL_MANAGER = "java.util.logging.manager",
		VALUE_JUL_MANAGER = "org.apache.logging.log4j.jul.LogManager",
		//
		KEY_THREAD_CTX_INHERIT = "isThreadContextMapInheritable",
		VALUE_THREAD_CTX_INHERIT = Boolean.toString(true),
		//
		FNAME_LOG_CONF = "logging.yaml";
	//
	private final static AtomicBoolean IS_LOG_INIT = new AtomicBoolean(false);
	static {
		if(IS_LOG_INIT.compareAndSet(false, true)) {
			//
			System.setProperty(KEY_THREAD_CTX_INHERIT, VALUE_THREAD_CTX_INHERIT);
			// set "run.id" property with timestamp value if not set before
			String runId = System.getProperty(RunTimeConfig.KEY_RUN_ID);
			if(runId == null || runId.length() == 0) {
				System.setProperty(
					RunTimeConfig.KEY_RUN_ID,
					Settings.FMT_DT.format(
						Calendar.getInstance(Settings.TZ_UTC, Settings.LOCALE_DEFAULT).getTime()
					)
				);
			}
			// make all used loggers asynchronous
			System.setProperty(KEY_LOG4J_CTX_SELECTOR, VALUE_LOG4J_CTX_SELECTOR);
			// connect JUL to Log4J2
			System.setProperty(KEY_JUL_MANAGER, VALUE_JUL_MANAGER);
			// determine the logger configuration file path
			final Path logConfPath = Paths.get(
				RunTimeConfig.DIR_ROOT, Constants.DIR_CONF, FNAME_LOG_CONF
			);
			//
			System.out.println(
				String.format(
					"Going to configure the logging subsystem using configuration file \"%s\"",
					logConfPath
				)
			);
			try {
				final LoggerContext logCtx = Configurator.initialize(
					"mongoose", logConfPath.toUri().toString()
				);
				if(logCtx == null) {
					System.err.println("Logging configuration failed");
				} else {
					LogManager.getLogger().info(
						Markers.MSG, "Logging subsystem is configured successfully"
					);
					Runtime.getRuntime().addShutdownHook(
						new Thread("logCtxShutDownHook") {
							@Override
							public final void run() {
								if(!logCtx.isStopped()) {
									logCtx.stop();
								}
							}
						}
					);
				}
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	//
	public final static Marker
		MSG = MarkerManager.getMarker("msg"),
		ERR = MarkerManager.getMarker("err"),
		DATA_LIST = MarkerManager.getMarker("dataList"),
		PERF_AVG = MarkerManager.getMarker("perfAvg"),
		PERF_SUM = MarkerManager.getMarker("perfSum"),
		PERF_TRACE = MarkerManager.getMarker("perfTrace");
	//
}
