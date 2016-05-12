package com.emc.mongoose.run.cli;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-api.jar
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
// mongoose-server-api.jar
import com.emc.mongoose.run.webserver.WebUiRunner;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
// mongoose-server-impl.jar
// mongoose-storage-mock.jar
//
import com.emc.mongoose.storage.mock.impl.http.Cinderella;
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 04.07.14.
 Mongoose entry point.
 */
public final class ModeDispatcher {
	//
	public static void main(final String args[]) {
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		//
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		// load the config from CLI arguments
		final Map<String, String> properties = HumanFriendlyCli.parseCli(args);
		if(properties != null) {
			for(final String propKey : properties.keySet()) {
				appConfig.setProperty(propKey, properties.get(propKey));
			}
		}
		//
		final String runMode;
		if(args == null || args.length == 0 || args[0].startsWith("-")) {
			runMode = Constants.RUN_MODE_STANDALONE;
		} else {
			runMode = args[0];
		}
		appConfig.setRunMode(runMode);
		rootLogger.info(Markers.MSG, appConfig.toString());
		//
		switch(runMode) {
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				rootLogger.debug(Markers.MSG, "Starting the server");
				try {
					final LoadBuilderSvc multiSvc = new MultiLoadBuilderSvc(appConfig);
					multiSvc.start();
					multiSvc.await();
				} catch(final RemoteException | InterruptedException e) {
					LogUtil.exception(
						rootLogger, Level.ERROR, e, "Failed to run the load builder services"
					);
				}
				break;
			case Constants.RUN_MODE_WEBUI:
				rootLogger.debug(Markers.MSG, "Starting the web UI");
				new WebUiRunner().run();
				break;
			case Constants.RUN_MODE_WSMOCK:
			case Constants.RUN_MODE_CINDERELLA:
				rootLogger.debug(Markers.MSG, "Starting cinderella");
				try {
					new Cinderella(appConfig).run();
				} catch (final Exception e) {
					LogUtil.exception(rootLogger, Level.FATAL, e, "Failed to init cinderella");
				}
				break;
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_STANDALONE:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				try {
					new ScenarioRunner(appConfig).run();
				} catch(final Exception e) {
					LogUtil.exception(rootLogger, Level.FATAL, e, "Scenario failed");
					e.printStackTrace(System.out);
				}
				break;
			default:
				throw new IllegalArgumentException(
					String.format("Incorrect run mode: \"%s\"", runMode)
				);
		}
		//
		ServiceUtil.shutdown();
		LogUtil.shutdown();
	}
}
//
