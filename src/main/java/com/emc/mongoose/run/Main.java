package com.emc.mongoose.run;
//
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.web.load.server.impl.BasicLoadBuilderSvc;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.omg.SendingContext.RunTime;
//
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.Policy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
/**
 Created by kurila on 04.07.14.
 Mongoose entry point.
 */
public final class Main {
	//
	public final static String
		DOT = ".",
		SEP = System.getProperty("file.separator"),
		DIR_ROOT,
		DIR_CONF = "conf",
		DIR_LOGGING = "logging",
		DIR_PROPERTIES = "properties",
		FNAME_LOGGING_LOCAL = "local.json",
		FNAME_LOGGING_REMOTE = "remote.json",
		FNAME_POLICY = "security.policy",
		//
		KEY_DIR_ROOT = "dir.root",
		KEY_POLICY = "java.security.policy",
		KEY_RUN_ID = "run.id",
		KEY_RUN_MODE = "run.mode",
		//
		RUN_MODE_STANDALONE = "standalone",
		RUN_MODE_CLIENT = "client",
		RUN_MODE_COMPAT_CLIENT = "controller",
		RUN_MODE_SERVER = "server",
		RUN_MODE_COMPAT_SERVER = "driver",
		RUN_MODE_WEBUI = "webui",
		RUN_MODE_WSMOCK = "wsmock";
	//
	private final static DateFormat FMT_DT = new SimpleDateFormat(
		"yyyy.MM.dd.HH.mm.ss.SSS", Locale.ROOT
	);
	//
	public final static File JAR_SELF;
	static {
		String dirRoot = System.getProperty("user.dir");
		File jarSelf = null;
		try {
			jarSelf = new File(
				Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()
			);
			dirRoot = URLDecoder.decode(
				jarSelf.getParent(),
				StandardCharsets.UTF_8.displayName()
			);
		} catch(final UnsupportedEncodingException|URISyntaxException e) {
			e.printStackTrace(System.err);
		} finally {
			DIR_ROOT = dirRoot;
			JAR_SELF = jarSelf;
		}
	}
	//
	public static RunTimeConfig RUN_TIME_CONFIG;
	//
	public static void main(final String args[]) {
		//
		initSecurity();
		//
		final String runMode;
		if(args==null || args.length==0) {
			runMode = RUN_MODE_STANDALONE;
		} else {
			runMode = args[0];
		}
		//

		System.setProperty(KEY_RUN_MODE, runMode);
		final Logger rootLogger = initLogging(runMode);
		if(rootLogger==null) {
			System.err.println("Logging initialization failure");
			System.exit(1);
		}
		//
		ThreadContextMap.initThreadContextMap();
		//
		rootLogger.info(
			Markers.MSG, "Run in mode \"{}\", id: \"{}\"",
			System.getProperty(KEY_RUN_MODE), System.getProperty(KEY_RUN_ID)
		);
		// load the properties
		RUN_TIME_CONFIG = new RunTimeConfig();
		//
		RUN_TIME_CONFIG.loadPropsFromDir(Paths.get(DIR_ROOT, DIR_CONF, DIR_PROPERTIES));
		rootLogger.debug(Markers.MSG, "Loaded the properties from the files");
		RUN_TIME_CONFIG.loadSysProps();
		rootLogger.debug(Markers.MSG, "Loaded the system properties");
		//
		//ThreadContextMap.initThreadContextMap(RUN_TIME_CONFIG);
		//
		switch (runMode) {
			case RUN_MODE_SERVER:
			case RUN_MODE_COMPAT_SERVER:
				rootLogger.debug(Markers.MSG, "Starting the server");
				try(
					final WSLoadBuilderSvc<WSObject, WSLoadExecutor<WSObject>>
						loadBuilderSvc = new BasicLoadBuilderSvc<>()
				) {
					loadBuilderSvc.start();
					loadBuilderSvc.join();
				} catch(final IOException e) {
					ExceptionHandler.trace(rootLogger, Level.ERROR, e, "Load builder service failure");
				} catch(InterruptedException e) {
					rootLogger.debug(Markers.MSG, "Interrupted load builder service");
				}
				break;
			case RUN_MODE_WEBUI:
				rootLogger.debug(Markers.MSG, "Starting the web UI");
                    new JettyRunner(RUN_TIME_CONFIG).run();
				break;
			case RUN_MODE_WSMOCK:
				rootLogger.debug(Markers.MSG, "Starting the web storage mock");
				try {
					new WSMockServlet(RUN_TIME_CONFIG).run();
				} catch (final Exception e) {
					ExceptionHandler.trace(rootLogger, Level.FATAL, e, "Failed");
				}
				break;
			case RUN_MODE_CLIENT:
			case RUN_MODE_STANDALONE:
			case RUN_MODE_COMPAT_CLIENT:
				new Scenario(RUN_TIME_CONFIG).run();
				break;
			default:
				throw new IllegalArgumentException(
					String.format("Incorrect run mode: \"%s\"", runMode)
				);
		}
		//
		((LifeCycle) LogManager.getContext()).stop();
		System.exit(0);
	}
	//
	public static Logger initLogging(final String runMode) {
		//
		System.setProperty("isThreadContextMapInheritable", "true");
		// set "dir.root" property
		System.setProperty(KEY_DIR_ROOT, DIR_ROOT);
		// set "run.id" property with timestamp value if not set before
		String runId = System.getProperty(KEY_RUN_ID);
		if(runId==null || runId.length()==0) {
			System.setProperty(
				KEY_RUN_ID, FMT_DT.format(
					Calendar.getInstance(TimeZone.getTimeZone("GMT+0")).getTime()
				)
			);
		}
		// make all used loggers asynchronous
		System.setProperty(
			"Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
		);
		// determine the logger configuration file path
		final Path logConfPath = Paths.get(
			DIR_ROOT, DIR_CONF, DIR_LOGGING,
			(
				runMode.equals(RUN_MODE_STANDALONE) ||
				runMode.equals(RUN_MODE_CLIENT) ||
				runMode.equals(RUN_MODE_COMPAT_CLIENT)
			) ?
				FNAME_LOGGING_LOCAL : FNAME_LOGGING_REMOTE
		);
		// go
		Configurator.initialize(null, logConfPath.toUri().toString());
		return LogManager.getRootLogger();
	}
	//
	public static void initSecurity() {
        // load the security policy
        final String secPolicyURL = "file:" + DIR_ROOT + SEP + DIR_CONF + SEP + FNAME_POLICY;
        System.setProperty(KEY_POLICY, secPolicyURL);
        Policy.getPolicy().refresh();
        System.setSecurityManager(new SecurityManager());
    }
	//
}
//
