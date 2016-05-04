package com.emc.mongoose.webui;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.JsonUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.storage.mock.impl.http.Cinderella;
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created on 22.04.16.
 */
public class RunServlet extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();

	private static final String WSMOCK_MODE_NAME = "Cindirella";
	private static final String STANDALONE_MODE_NAME = "Mongoose";
	private static final String CLIENT_MODE_NAME = "client";
	private static final String SERVER_MODE_NAME = "server";

	private static final String APP_CONFIG_KEY = "config";
	private static final String SCENARIO_KEY = "scenario";
	private static final String RUN_ID_KEY = "runId";
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	private static final Map<String, Thread> TESTS = new HashMap<>();
	private static final Map<String, String> MODES = new LinkedHashMap<>();

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final Map<String, Map<String, Object>> startProperties = getStartProperties(request);
		final AppConfig config = getConfig(startProperties);
		final JsonScenario scenario = getScenario(startProperties, config);
		String runId = getRunId(config);
		if (!isRunIdFree(runId)) {
			try {
				response.getWriter().write("Run.id is occupied already");
			} catch (final IOException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to write in servlet response");
			}
			return;
		}
		runId =  LogUtil.newRunId();
		config.setProperty(AppConfig.KEY_RUN_ID, runId);
		final String runMode = getRunMode(config);
		switch (runMode) {
			case Constants.RUN_MODE_STANDALONE:
				runTest(runId, new StandaloneLikeRunner(config, scenario, STANDALONE_MODE_NAME), runMode);
				break;
			case Constants.RUN_MODE_WSMOCK:
				runTest(runId, new WsMockRunner(config), runMode);
				break;
			case Constants.RUN_MODE_SERVER:
				runTest(runId, new ServerRunner(config), runMode);
				break;
			case Constants.RUN_MODE_CLIENT:
				runTest(runId, new StandaloneLikeRunner(config, scenario, CLIENT_MODE_NAME), runMode);
				break;
			default:
				runTest(runId, new StandaloneLikeRunner(config, scenario, STANDALONE_MODE_NAME), runMode);
		}
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		response.getWriter().write(JSON_MAPPER.writeValueAsString(MODES));
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String runId = getPlainJson(request).get(RUN_ID_KEY);
		stopAndRemoveTest(runId);
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		response.getWriter().write(JSON_MAPPER.writeValueAsString(MODES));
	}

	private Map<String, Map<String, Object>> getStartProperties(final HttpServletRequest request) throws IOException {
		final String startPropertiesString;
		try (
				final BufferedReader reader = request.getReader()
		) {
			startPropertiesString = JsonUtil.readString(reader);
		}
		return JSON_MAPPER.readValue(
				startPropertiesString, new TypeReference<Map<String, Object>>() {
				}
		);
	}

	private Map<String, String> getPlainJson(final HttpServletRequest request) throws IOException {
		final String plainJsonString;
		try (
				final BufferedReader reader = request.getReader()
		) {
			plainJsonString = JsonUtil.readString(reader);
		}
		return JSON_MAPPER.readValue(
				plainJsonString, new TypeReference<Map<String, String>>() {
				}
		);
	}

	private AppConfig getConfig(final Map<String, Map<String, Object>> startProperties)
			throws JsonProcessingException {
		final Map<String, Object> appConfig = startProperties.get(APP_CONFIG_KEY);
		final byte[] configJsonBytes = JSON_MAPPER.writeValueAsBytes(appConfig);
		return new BasicConfig(configJsonBytes);
	}

	private JsonScenario getScenario(final Map<String, Map<String, Object>> startProperties,
	                                 final AppConfig config)
			throws IOException {
		final Map<String, Object> scenarioMap = startProperties.get(SCENARIO_KEY);
		JsonScenario scenario = null;
		if (scenarioMap != null) {
			try {
				scenario = new JsonScenario(config, scenarioMap);
			} catch (CloneNotSupportedException e) {
				LOG.error("Failed to parse the scenario", e);
			}
		}
		return scenario;
	}

	private String getRunId(final AppConfig config) {
		return config.getRunId();
	}

	private String getRunMode(final AppConfig config) {
		return config.getRunMode();
	}

	private boolean isRunIdFree(final String runId) {
		return !TESTS.containsKey(runId);
	}

	@SuppressWarnings("unchecked")
	private void runTest(final String runId, final Runner runner, final String runMode) {
		final Thread test = new Thread(runner, runId);
		test.start();
		putTest(runId, test, runMode);
	}

	private void putTest(final String runId, final Thread test, final String mode) {
		TESTS.put(runId, test);
		MODES.put(runId, mode);
	}

	private void stopAndRemoveTest(final String runId) {
		TESTS.get(runId).interrupt();
		TESTS.remove(runId);
		MODES.remove(runId);
	}

	private static abstract class Runner implements Runnable {

		private final AppConfig config;
		private final String startMessage;
		private final String failMessage;

		Runner(final AppConfig config, final String startMessage, final String failMessage) {
			this.config = config;
			this.startMessage = startMessage;
			this.failMessage = failMessage;
		}

		Runner(final AppConfig config, final String modeName) {
			this(config, defaultStartMessage(modeName), defaultFailMessage(modeName));
		}

		AppConfig getConfig() {
			return config;
		}

		void logStart() {
			LOG.debug(Markers.MSG, startMessage);
		}

		void logFail(final Exception e) {
			LogUtil.exception(LOG, Level.FATAL, e, failMessage);
		}

		abstract void start() throws Exception;

		@Override
		public void run() {
			BasicConfig.THREAD_CONTEXT.set(config);
			logStart();
			try {
				start();
			} catch (final Exception e) {
				logFail(e);
			}
		}

		private static String defaultStartMessage(final String object) {
			return "Starting " + object;
		}

		private static String defaultFailMessage(final String object) {
			return "Failed to start " + object;
		}
	}

	private static class WsMockRunner extends Runner {

		WsMockRunner(final AppConfig config) {
			super(config, WSMOCK_MODE_NAME);
		}

		@Override
		void start() throws Exception {
			new Cinderella(super.config).run();
		}

	}

	private static class StandaloneLikeRunner extends Runner {

		private Scenario scenario;

		StandaloneLikeRunner(final AppConfig config, final Scenario scenario,
		                     final String standaloneLikeModeName) {
			super(config, standaloneLikeModeName);
			this.scenario = scenario;
		}

		@Override
		void start() throws Exception {
			new ScenarioRunner(scenario).run();
		}

	}

	private static class ServerRunner extends Runner {

		ServerRunner(final AppConfig config) {
			super(config, SERVER_MODE_NAME);
		}

		@Override
		void start() throws Exception {
			LoadBuilderSvc multiSvc = new MultiLoadBuilderSvc(super.config);
			multiSvc.start();
			// todo why is there no multiSvc.await()?
		}

	}


}
