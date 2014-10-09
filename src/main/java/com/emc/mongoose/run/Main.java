package com.emc.mongoose.run;
//
import com.emc.mongoose.object.load.server.WSLoadBuilderSvcImpl;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
//
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
		FEXT_JSON = Main.DOT + "json",
		FNAME_POLICY = "security.policy",
		//
		KEY_DIR_ROOT = "dir.root",
		KEY_POLICY = "java.security.policy",
		KEY_RUN_ID = "run.id",
		KEY_RUN_MODE = "run.mode",
		//
		VALUE_RUN_MODE_STANDALONE = "standalone",
		VALUE_RUN_MODE_CLIENT = "client",
		VALUE_RUN_MODE_COMPAT_CLIENT = "controller",
		VALUE_RUN_MODE_SERVER = "server",
		VALUE_RUN_MODE_COMPAT_SERVER = "driver",
		VALUE_RUN_MODE_WEBUI = "webui",
		VALUE_RUN_MODE_WSMOCK = "wsmock";
	//
	private final static DateFormat FMT_DT = new SimpleDateFormat(
		"yyyy.MM.dd.HH.mm.ss.SSS", Locale.ROOT
	);
	//
	public final static File
		JAR_SELF;
	//
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
	public static void main(final String args[]) {
		//
		initSecurity();
		//
		final String runMode;
		if(args==null || args.length==0) {
			runMode = VALUE_RUN_MODE_STANDALONE;
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
		rootLogger.info(
			Markers.MSG, "Logging configured, run.id=\"{}\"", System.getProperty(KEY_RUN_ID)
		);
		// load the properties
		RunTimeConfig.loadPropsFromDir(Paths.get(DIR_ROOT, DIR_CONF, DIR_PROPERTIES));
		rootLogger.debug(Markers.MSG, "Loaded the properties from the files");
		RunTimeConfig.loadSysProps();
		rootLogger.debug(Markers.MSG, "Loaded the system properties");
		//
		switch (runMode) {
			case VALUE_RUN_MODE_SERVER:
			case VALUE_RUN_MODE_COMPAT_SERVER:
				rootLogger.debug(Markers.MSG, "Starting the server");
				new WSLoadBuilderSvcImpl().start();
				break;
			case VALUE_RUN_MODE_WEBUI:
				rootLogger.debug(Markers.MSG, "Starting the web UI");
                JettyRunner.run();
				break;
			case VALUE_RUN_MODE_WSMOCK:
				rootLogger.debug(Markers.MSG, "Starting the web storage mock");
				try {
					WSMock.run();
				} catch (final Exception e) {
					ExceptionHandler.trace(rootLogger, Level.FATAL, e, "Failed");
				}
				break;
			case VALUE_RUN_MODE_CLIENT:
			case VALUE_RUN_MODE_STANDALONE:
			case VALUE_RUN_MODE_COMPAT_CLIENT:
				Scenario.run();
				System.exit(0);
				break;
			default:
				throw new IllegalArgumentException(
					String.format("Incorrect run mode: \"%s\"", runMode)
				);
		}
		//
	}
	//
	public static Logger initLogging(final String runMode) {
		// seet "dir.root" property
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
		// load the logging configuration
		final Path logConfPath = Paths.get(
			DIR_ROOT, DIR_CONF, DIR_LOGGING, runMode+FEXT_JSON
		);
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
