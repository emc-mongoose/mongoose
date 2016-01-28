package com.emc.mongoose.run.cli;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-api.jar
import com.emc.mongoose.run.webserver.WUIRunner;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
// mongoose-server-impl.jar
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
//
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
		// load the config from CLI arguments
		//final Map<String, String> properties = HumanFriendly.parseCli(args);
		//
		final String runMode;
		if(args == null || args.length == 0 || args[0].startsWith("-")) {
			runMode = Constants.RUN_MODE_STANDALONE;
		} else {
			runMode = args[0];
		}
		System.setProperty(AppConfig.KEY_RUN_MODE, runMode);
		LogUtil.init();
		//
		final Logger rootLogger = LogManager.getRootLogger();
		rootLogger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//
		switch(runMode) {
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				rootLogger.debug(Markers.MSG, "Starting the server");
				try {
					final LoadBuilderSvc multiSvc = new MultiLoadBuilderSvc(
						BasicConfig.THREAD_CONTEXT.get()
					);
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
				new WUIRunner(BasicConfig.THREAD_CONTEXT.get()).run();
				break;
			case Constants.RUN_MODE_CINDERELLA:
			case Constants.RUN_MODE_WSMOCK:
				rootLogger.debug(Markers.MSG, "Starting the cinderella");
				try {
					new Cinderella(BasicConfig.THREAD_CONTEXT.get()).run();
				} catch (final Exception e) {
					LogUtil.exception(rootLogger, Level.FATAL, e, "Failed to init the cinderella");
				}
				break;
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_STANDALONE:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				runScenario();
				break;
			default:
				throw new IllegalArgumentException(
					String.format("Incorrect run mode: \"%s\"", runMode)
				);
		}
		//
		ServiceUtil.shutdown();
	}

	private static void runScenario() {
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		if (appConfig != null) {
			// TODO
			LogManager.getRootLogger().info(Markers.MSG, "Scenario end");
		} else {
			throw new NullPointerException(
				"appConfig hasn't been initialized"
			);
		}
	}
}
//
