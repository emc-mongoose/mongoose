package com.emc.mongoose.run.cli;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
// mongoose-scenario.jar
import com.emc.mongoose.run.scenario.ScriptRunner;
import com.emc.mongoose.run.webserver.WUIRunner;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
// mongoose-server-impl.jar
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.impl.Cinderella;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
/**
 Created by kurila on 04.07.14.
 Mongoose entry point.
 */
public final class ModeDispatcher {
	//
	public static void main(final String args[]) {
		//
		final String runMode;
		if(args == null || args.length == 0 || args[0].startsWith("-")) {
			runMode = Constants.RUN_MODE_STANDALONE;
		} else {
			runMode = args[0];
		}
		System.setProperty(RunTimeConfig.KEY_RUN_MODE, runMode);
		LogUtil.init();
		//
		final Logger rootLogger = LogManager.getRootLogger();
		//
		RunTimeConfig.initContext();
		// load the config from file
		RunTimeConfig.getContext().loadPropsFromJsonCfgFile(
			Paths.get(RunTimeConfig.DIR_ROOT, Constants.DIR_CONF).resolve(RunTimeConfig.FNAME_CONF)
		);
		rootLogger.debug(Markers.MSG, "Loaded the properties from the files");
		// load the config from system properties
		RunTimeConfig.getContext().loadSysProps();
		// load the config from CLI arguments
		final Map<String, String> properties = HumanFriendly.parseCli(args);
		if(!properties.isEmpty()) {
			rootLogger.debug(Markers.MSG, "Overriding properties {}", properties);
			RunTimeConfig.getContext().overrideSystemProperties(properties);
		}
		//
		rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		switch(runMode) {
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				rootLogger.debug(Markers.MSG, "Starting the server");
				try(
					final WSLoadBuilderSvc<WSObject, WSLoadExecutor<WSObject>>
						loadBuilderSvc = new BasicWSLoadBuilderSvc<>(RunTimeConfig.getContext())
				) {
					loadBuilderSvc.start();
					loadBuilderSvc.await();
				} catch(final IOException e) {
					LogUtil.exception(rootLogger, Level.ERROR, e, "Load builder service failure");
				} catch(InterruptedException e) {
					rootLogger.debug(Markers.MSG, "Interrupted load builder service");
				}
				break;
			case Constants.RUN_MODE_WEBUI:
				rootLogger.debug(Markers.MSG, "Starting the web UI");
				new WUIRunner(RunTimeConfig.getContext()).run();
				break;
			case Constants.RUN_MODE_CINDERELLA:
			case Constants.RUN_MODE_WSMOCK:
				rootLogger.debug(Markers.MSG, "Starting the cinderella");
				try {
					new Cinderella(RunTimeConfig.getContext()).run();
				} catch (final Exception e) {
					LogUtil.exception(rootLogger, Level.FATAL, e, "Failed");
				}
				break;
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_STANDALONE:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				new ScriptRunner().run();
				break;
			default:
				throw new IllegalArgumentException(
					String.format("Incorrect run mode: \"%s\"", runMode)
				);
		}
		//
		ServiceUtils.shutdown();
		LogUtil.shutdown();
	}
}
//
