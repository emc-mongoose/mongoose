package com.emc.mongoose.run.cli;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-api.jar
// mongoose-scenario.jar
import com.emc.mongoose.run.scenario.Chain;
import com.emc.mongoose.run.scenario.Rampup;
import com.emc.mongoose.run.scenario.Single;
import com.emc.mongoose.run.webserver.WUIRunner;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
// mongoose-server-impl.jar
// mongoose-storage-mock.jar
//
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import com.emc.mongoose.storage.mock.impl.web.Nagaina;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 04.07.14.
 Mongoose entry point.
 */
public final class ModeDispatcher {
	//
	public static void main(final String args[]) {
		// load the config from CLI arguments
		final Map<String, String> properties = HumanFriendly.parseCli(args);
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
		if(properties != null && !properties.isEmpty()) {
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
				final LoadBuilderSvc multiSvc = new MultiLoadBuilderSvc(RunTimeConfig.getContext());
				try {
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
				new WUIRunner(RunTimeConfig.getContext()).run();
				break;
			case Constants.RUN_MODE_NAGAINA:
			case Constants.RUN_MODE_WSMOCK:
				rootLogger.debug(Markers.MSG, "Starting nagaina");
				try {
					 new Nagaina(RunTimeConfig.getContext()).run();
				} catch (final Exception e) {
					LogUtil.exception(rootLogger, Level.FATAL, e, "Failed to init nagaina");
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
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		if (rtConfig != null) {
			final String scenarioName = rtConfig.getScenarioName();
			switch (scenarioName) {
				case Constants.RUN_SCENARIO_SINGLE:
					new Single(rtConfig).run();
					break;
				case Constants.RUN_SCENARIO_CHAIN:
					new Chain(rtConfig).run();
					break;
				case Constants.RUN_SCENARIO_RAMPUP:
					new Rampup(rtConfig).run();
					break;
				default:
					throw new IllegalArgumentException(
						String.format("Incorrect scenario: \"%s\"", scenarioName)
					);
			}
			LogManager.getRootLogger().info(Markers.MSG, "Scenario end");
		} else {
			throw new NullPointerException(
				"runTimeConfig hasn't been initialized"
			);
		}
	}
}
//
