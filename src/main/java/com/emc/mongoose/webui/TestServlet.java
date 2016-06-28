package com.emc.mongoose.webui;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.json.JsonUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.impl.http.Cinderella;
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.emc.mongoose.webui.ServletConstants.RUN_ID_KEY;

/**
 * Created on 22.04.16.
 */
public class TestServlet
	extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();

	private static final String WSMOCK_MODE_NAME = "WSMock";
	private static final String STANDALONE_MODE_NAME = "Mongoose";
	private static final String CLIENT_MODE_NAME = "client";
	private static final String SERVER_MODE_NAME = "server";

	private static final String APP_CONFIG_KEY = "config";
	private static final String SCENARIO_KEY = "scenario";
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	private static final Map<String, Thread> TESTS = new HashMap<>();
	private static final Map<String, String> MODES = new LinkedHashMap<>();
	private static final String MODE_KEY = "mode";
	private static final String STATUS_KEY = "status";

	private enum Status {
		RUNNING, STOPPED
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final Map<String, Map<String, Object>> startProperties = getStartProperties(request);
		final AppConfig config = getConfig(startProperties);
		String runId = config.getRunId();
		if(runId == null || runId.length() == 0) {
			runId = LogUtil.newRunId();
			config.setRunId(runId);
		}
		JsonScenario scenario = null;
		if (startProperties.get(SCENARIO_KEY) != null) {
			config.setProperty(AppConfig.KEY_SCENARIO_FROM_WEBUI, true);
			scenario = getScenario(startProperties, config);
		}
		final JsonScenario finalScenario = scenario;
		final String runMode = config.getRunMode();
		switch (runMode) {
			case Constants.RUN_MODE_WSMOCK:
				startTest(runId, new Runnable() {
					@Override
					public void run() {
						logStart(WSMOCK_MODE_NAME);
						try(final StorageMock sm = new Cinderella(config)) {
							sm.run();
						} catch(final IOException e) {
							logFail(WSMOCK_MODE_NAME);
						}
					}
				}, runMode);
				break;
			case Constants.RUN_MODE_SERVER:
				startTest(runId, new Runnable() {
					@Override
					public void run() {
						try(final LoadBuilderSvc multiSvc = new MultiLoadBuilderSvc(config)) {
							logStart(SERVER_MODE_NAME);
							multiSvc.start();
							multiSvc.await();
						} catch(final IOException | InterruptedException e) {
							logFail(SERVER_MODE_NAME);
						}
					}
				}, runMode);
				break;
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_STANDALONE:
			default:
				startTest(runId, new Runnable() {
					@Override
					public void run() {
						logStart(runMode);
						try(final ScenarioRunner sr = new ScenarioRunner(config, finalScenario)) {
							sr.run();
						} catch(final IOException e) {
							logFail(runMode);
						}
					}
				}, runMode);
		}
		sendRunIdInfo(response);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
	throws ServletException, IOException {
		try (
				final BufferedReader reader = request.getReader()
		) {
			final String runId = JsonUtil.readValue(reader).get(RUN_ID_KEY);
			stopTest(runId);
		}
	}

	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response)
	throws ServletException, IOException {
		try (
			final BufferedReader reader = request.getReader()
		) {
			final String runId = JsonUtil.readValue(reader).get(RUN_ID_KEY);
			if (TESTS.get(runId).isAlive()) {
				stopTest(runId);
			}
			removeTest(runId);
		}
	}

	private Map<String, Map<String, String>> collectRunIdInfo() {
		final Map<String, Map<String, String>> runIdInfo = new LinkedHashMap<>();
		for (final Map.Entry<String, String> modeEntry: MODES.entrySet()) {
			final String runId = modeEntry.getKey();
			runIdInfo.put(runId, new HashMap<String, String>());
			final Map<String, String> runIdMap = runIdInfo.get(runId);
			runIdMap.put(MODE_KEY, modeEntry.getValue());
			if (TESTS.get(runId).isAlive()) {
				runIdMap.put(STATUS_KEY, Status.RUNNING.name());
			} else {
				runIdMap.put(STATUS_KEY, Status.STOPPED.name());
			}
		}
		return runIdInfo;
	}

	private void sendRunIdInfo(final HttpServletResponse response)
	throws IOException {
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		response.getWriter().write(JSON_MAPPER.writeValueAsString(collectRunIdInfo()));
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
	throws ServletException, IOException {
		sendRunIdInfo(response);
	}

	private Map<String, Map<String, Object>> getStartProperties(final HttpServletRequest request)
	throws IOException {
		final String startPropertiesString;
		try (
				final BufferedReader reader = request.getReader()
		) {
			startPropertiesString = JsonUtil.readString(reader);
		}
		return JSON_MAPPER.readValue(startPropertiesString, JsonUtil.COMPLEX_JSON_TYPE);
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
			} catch (final CloneNotSupportedException e) {
				LOG.error("Failed to parse the scenario", e);
			}
		}
		return scenario;
	}

	@SuppressWarnings("unchecked")
	private void startTest(final String runId, final Runnable testTask, final String runMode) {
		final Thread test = new Thread(testTask, runId);
		test.setName("run<" + runId + ">");
		test.start();
		putTest(runId, test, runMode);
	}

	private void putTest(final String runId, final Thread test, final String mode) {
		TESTS.put(runId, test);
		MODES.put(runId, mode);
	}

	private void stopTest(final String runId) {
		TESTS.get(runId).interrupt();
	}

	private void removeTest(final String runId) {
		TESTS.remove(runId);
		MODES.remove(runId);
	}

	private static String logStart(final String object) {
		return "Starting " + object;
	}

	private static String logFail(final String object) {
		return "Failed to start " + object;
	}
}
