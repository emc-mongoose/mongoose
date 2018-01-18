package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.ui.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.ui.cli.CliArgParser.getAllCliArgs;

import org.apache.logging.log4j.CloseableThreadContext;

import java.util.Arrays;

public final class Main {

	public static void main(final String... args)
	throws Exception {

		LogUtil.init();

		final Config config = Config.loadDefaults();
		if(config == null) {
			throw new AssertionError();
		}

		try {
			config.apply(
				CliArgParser.parseArgs(config.getAliasingConfig(), args),
				"none-" + LogUtil.getDateTimeStamp()
			);
		} catch(final IllegalArgumentNameException e) {
			System.err.println(
				"Invalid argument: \"" + e.getMessage() + "\"\nThe list of all possible args:\n"
					+ formatCliArgsList(getAllCliArgs())
			);
			return;
		}

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, config.getTestConfig().getStepConfig().getId())
				.put(KEY_CLASS_NAME, com.emc.mongoose.scenario.Main.class.getSimpleName())
		) {
			Arrays.stream(args).forEach(Loggers.CLI::info);
			Loggers.CONFIG.info(config.toString());
			final int listenPort = config.getTestConfig().getStepConfig().getNodeConfig().getPort();
			Service inputFileSvc = null;
			Service scenarioStepSvc = null;
			try {
				inputFileSvc = new BasicFileManagerService(listenPort);
				inputFileSvc.start();
				scenarioStepSvc = new BasicScenarioStepManagerService(listenPort);
				scenarioStepSvc.start();
				scenarioStepSvc.await();
			} finally {
				if(inputFileSvc != null) {
					inputFileSvc.close();
				}
				if(scenarioStepSvc != null) {
					scenarioStepSvc.close();
				}
			}
		}
	}
}
