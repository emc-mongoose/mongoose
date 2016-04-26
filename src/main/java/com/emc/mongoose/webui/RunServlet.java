package com.emc.mongoose.webui;

import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.io.JsonUtil;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * Created on 22.04.16.
 */
public class RunServlet extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();

	private static final String APP_CONFIG_KEY = "config";
	private static final String SCENARIO_KEY = "scenario";
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final String startJson;
		try (
				final BufferedReader reader = request.getReader()
		) {
			startJson = JsonUtil.readString(reader);
		}
		final Map<String, Map<String, Object>> startMap = JSON_MAPPER.readValue(
						startJson, new TypeReference<Map<String, Object>>(){}
				);
		final Map<String, Object> appConfig = startMap.get(APP_CONFIG_KEY);
		final byte[] configJsonBytes = JSON_MAPPER.writeValueAsBytes(appConfig);
		final BasicConfig basicConfig = new BasicConfig(configJsonBytes);
		BasicConfig.THREAD_CONTEXT.set(basicConfig);
		final Map<String, Object> scenarioMap = startMap.get(SCENARIO_KEY);
		JsonScenario scenario = null;
		try {
			scenario = new JsonScenario(basicConfig, scenarioMap);
		} catch (CloneNotSupportedException e) {
			LOG.error("Failed to parse the scenario", e);
		}
		if (scenario != null) {
			new ScenarioRunner(scenario).run();
			response.getWriter().write("Mongoose ran");
		} else {
			response.getWriter().write("Failed to start Mongoose");
		}

	}


}
