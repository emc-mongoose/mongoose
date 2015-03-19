package com.emc.mongoose.run.main;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.Constants;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.logging.TraceLogger;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
//
import com.emc.mongoose.run.cli.HumanFriendly;
import com.emc.mongoose.run.scenario.RunTask;
import com.emc.mongoose.run.webserver.RunJettyTask;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
//
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
//
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configurator;
//
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Map;
/**
 Created by kurila on 04.07.14.
 Mongoose entry point.
 */
public final class Launcher {
	//
	public static void main(final String args[]) {
		//
		//initSecurity();
		//
		final String runMode;
		if(args == null || args.length == 0 || args[0].startsWith("-")) {
			runMode = com.emc.mongoose.common.conf.Constants.RUN_MODE_STANDALONE;
		} else {
			runMode = args[0];
		}
		System.setProperty(RunTimeConfig.KEY_RUN_MODE, runMode);
		//
		final Map<String, String> properties = HumanFriendly.parseCli(args);
		//
		initLogging(runMode);
		final Logger rootLogger = LogManager.getRootLogger();
		if(rootLogger == null) {
			StatusLogger.getLogger().fatal("Logging initialization failure");
		}
		//
		RunTimeConfig.initContext();
		// load the properties
		RunTimeConfig.getContext().loadPropsFromDir(
			Paths.get(
				RunTimeConfig.DIR_ROOT,
				com.emc.mongoose.common.conf.Constants.DIR_CONF,
				com.emc.mongoose.common.conf.Constants.DIR_PROPERTIES
			)
		);
		rootLogger.debug(Markers.MSG, "Loaded the properties from the files");
		RunTimeConfig.getContext().loadSysProps();
		rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		if(!properties.isEmpty()) {
			rootLogger.info(Markers.MSG, "Overriding properties {}", properties);
			RunTimeConfig.getContext().overrideSystemProperties(properties);
		}
		//
		switch (runMode) {
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_SERVER:
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_COMPAT_SERVER:
				rootLogger.debug(Markers.MSG, "Starting the server");
				try(
					final WSLoadBuilderSvc<WSObject, WSLoadExecutor<WSObject>>
						loadBuilderSvc = new BasicWSLoadBuilderSvc<>(RunTimeConfig.getContext())
				) {
					loadBuilderSvc.start();
					loadBuilderSvc.join();
				} catch(final IOException e) {
					TraceLogger.failure(rootLogger, Level.ERROR, e, "Load builder service failure");
				} catch(InterruptedException e) {
					rootLogger.debug(Markers.MSG, "Interrupted load builder service");
				}
				break;
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_WEBUI:
				rootLogger.debug(Markers.MSG, "Starting the web UI");
				new RunJettyTask(RunTimeConfig.getContext()).run();
				break;
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_CINDERELLA:
				rootLogger.debug(Markers.MSG, "Starting the cinderella");
				try {
					new com.emc.mongoose.storage.mock.impl.cinderella.Main(
						RunTimeConfig.getContext()
					).run();
				} catch (final Exception e) {
					TraceLogger.failure(rootLogger, Level.FATAL, e, "Failed");
				}
				break;
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_CLIENT:
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_STANDALONE:
			case com.emc.mongoose.common.conf.Constants.RUN_MODE_COMPAT_CLIENT:
				new RunTask().run();
				break;
			default:
				throw new IllegalArgumentException(
					String.format("Incorrect run mode: \"%s\"", runMode)
				);
		}
		//
		shutdown();
	}
	//
	private static volatile LoggerContext LOG_CONTEXT = null;
	//
	public static void initLogging(final String runMode) {
		//
		System.setProperty("isThreadContextMapInheritable", "true");
		// set "run.id" property with timestamp value if not set before
		String runId = System.getProperty(RunTimeConfig.KEY_RUN_ID);
		if(runId == null || runId.length() == 0) {
			System.setProperty(
				RunTimeConfig.KEY_RUN_ID,
				Constants.FMT_DT.format(
					Calendar.getInstance(Constants.TZ_UTC, Constants.LOCALE_DEFAULT).getTime()
				)
			);
		}
		// make all used loggers asynchronous
		System.setProperty(
			"Log4jContextSelector", AsyncLoggerContextSelector.class.getCanonicalName()
		);
		// connect JUL to Log4J2
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		// determine the logger configuration file path
		final Path logConfPath = Paths.get(
			RunTimeConfig.DIR_ROOT,
			com.emc.mongoose.common.conf.Constants.DIR_CONF,
			"logging.yaml"
		);
		//
		LOG_CONTEXT = Configurator.initialize("mongoose", logConfPath.toUri().toString());
	}
	/*
	public static void initSecurity() {
		// load the security policy
		final String secPolicyURL = "file:" +
			RunTimeConfig.DIR_ROOT + File.separatorChar +
			com.emc.mongoose.common.conf.Constants.DIR_CONF + File.separatorChar +
			com.emc.mongoose.common.conf.Constants.FNAME_POLICY;
		System.setProperty(com.emc.mongoose.common.conf.Constants.KEY_POLICY, secPolicyURL);
		Policy.getPolicy().refresh();
		System.setSecurityManager(new SecurityManager());
	}*/
	//
	public static void shutdown() {
		if(!LOG_CONTEXT.isStopped()) {
			LOG_CONTEXT.stop();
		}
		ServiceUtils.shutdown();
	}
}
//
