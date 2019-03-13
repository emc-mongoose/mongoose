package com.emc.mongoose.base.control.run;

import static org.eclipse.jetty.http.MimeTypes.Type.MULTIPART_FORM_DATA;

import com.emc.mongoose.base.concurrent.SingleTaskExecutor;
import com.emc.mongoose.base.concurrent.SingleTaskExecutorImpl;
import com.emc.mongoose.base.config.ConfigUtil;
import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.load.step.ScenarioUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.metrics.MetricsManager;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.exceptions.InvalidValuePathException;
import com.github.akurilov.confuse.exceptions.InvalidValueTypeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.eclipse.jetty.http.HttpHeader;

/** @author veronika K. on 08.11.18 */
public class RunServlet extends HttpServlet {

	private static final String PART_KEY_DEFAULTS = "defaults";
	private static final String PART_KEY_SCENARIO = "scenario";

	private final ScriptEngine scriptEngine;
	private final List<Extension> extensions;
	private final MetricsManager metricsMgr;
	private final Config aggregatedConfigWithArgs;
	private final Path appHomePath;
	private final SingleTaskExecutor scenarioExecutor = new SingleTaskExecutorImpl();

	public RunServlet(
					final ClassLoader clsLoader,
					final List<Extension> extensions,
					final MetricsManager metricsMgr,
					final Config aggregatedConfigWithArgs,
					final Path appHomePath) {
		this.scriptEngine = ScenarioUtil.scriptEngineByDefault(clsLoader);
		this.extensions = extensions;
		this.metricsMgr = metricsMgr;
		this.aggregatedConfigWithArgs = aggregatedConfigWithArgs;
		this.appHomePath = appHomePath;
	}

	@Override
	protected final void doPost(final HttpServletRequest req, final HttpServletResponse resp)
					throws IOException, ServletException {

		final Part defaultsPart;
		final Part scenarioPart;
		final var contentTypeHeaderValue = req.getHeader(HttpHeader.CONTENT_TYPE.toString());
		if (contentTypeHeaderValue != null
						&& contentTypeHeaderValue.startsWith(MULTIPART_FORM_DATA.toString())) {
			defaultsPart = req.getPart(PART_KEY_DEFAULTS);
			scenarioPart = req.getPart(PART_KEY_SCENARIO);
		} else {
			defaultsPart = null;
			scenarioPart = null;
		}
		try {
			final var defaults = mergeIncomingWithLocalConfig(defaultsPart, resp, aggregatedConfigWithArgs);
			final var scenario = getIncomingScenarioOrDefault(scenarioPart, appHomePath);

			// expose the base configuration and the step types
			ScenarioUtil.configure(scriptEngine, extensions, defaults, metricsMgr);
			//
			final var run = (Run) new RunImpl(defaults.stringVal("run-comment"), scenario, scriptEngine);
			try {
				scenarioExecutor.execute(run);
				resp.setStatus(HttpServletResponse.SC_ACCEPTED);
				setRunTimestampHeader(run, resp);
			} catch (final RejectedExecutionException e) {
				resp.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		} catch (final NoSuchMethodException | RuntimeException e) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@Override
	protected final void doHead(final HttpServletRequest req, final HttpServletResponse resp) {
		applyForActiveRunIfAny(resp, RunServlet::setRunExistsResponse);
	}

	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp)
					throws IOException {
		extractRequestTimestampAndApply(
						req, resp, (run, timestamp) -> setRunMatchesResponse(run, resp, timestamp));
	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
					throws IOException {
		extractRequestTimestampAndApply(
						req,
						resp,
						(run, timestamp) -> stopRunIfMatchesAndSetResponse(run, resp, timestamp, scenarioExecutor));
	}

	static void setRunTimestampHeader(final Run task, final HttpServletResponse resp) {
		resp.setHeader(HttpHeader.ETAG.name(), Long.toString(task.timestamp(), 0x10));
	}

	void applyForActiveRunIfAny(
					final HttpServletResponse resp, final BiConsumer<Run, HttpServletResponse> action) {
		final var activeTask = scenarioExecutor.task();
		if (null == activeTask) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else if (activeTask instanceof Run) {
			final var activeRun = (Run) activeTask;
			action.accept(activeRun, resp);
		} else {
			Loggers.ERR.warn("The scenario executor runs an alien task: {}", activeTask);
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	void extractRequestTimestampAndApply(
					final HttpServletRequest req,
					final HttpServletResponse resp,
					final BiConsumer<Run, Long> runRespTimestampConsumer)
					throws IOException {
		final var reqTimestampRawValue = Collections.list(req.getHeaders(HttpHeader.IF_MATCH.toString())).stream()
						.findAny()
						.orElse(null);
		if (null == reqTimestampRawValue) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing header: " + HttpHeader.IF_MATCH);
		} else {
			try {
				final var reqTimestamp = Long.parseLong(reqTimestampRawValue, 0x10);
				applyForActiveRunIfAny(
								resp, (run, resp_) -> runRespTimestampConsumer.accept(run, reqTimestamp));
			} catch (final NumberFormatException e) {
				resp.sendError(
								HttpServletResponse.SC_BAD_REQUEST, "Invalid start time: " + reqTimestampRawValue);
			}
		}
	}

	static void setRunExistsResponse(final Run run, final HttpServletResponse resp) {
		resp.setStatus(HttpServletResponse.SC_OK);
		setRunTimestampHeader(run, resp);
	}

	static void setRunMatchesResponse(
					final Run run, final HttpServletResponse resp, final long reqTimestamp) {
		if (run.timestamp() == reqTimestamp) {
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	static void stopRunIfMatchesAndSetResponse(
					final Run run,
					final HttpServletResponse resp,
					final long reqTimestamp,
					final SingleTaskExecutor scenarioExecutor) {
		if (run.timestamp() == reqTimestamp) {
			scenarioExecutor.stop(run);
			if (null != scenarioExecutor.task()) {
				throw new AssertionError("Run stopping failure");
			}
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	static Config mergeIncomingWithLocalConfig(
					final Part defaultsPart,
					final HttpServletResponse resp,
					final Config aggregatedConfigWithArgs)
					throws IOException, NoSuchMethodException, InvalidValuePathException,
					InvalidValueTypeException {
		final Config configResult;
		if (defaultsPart == null) {
			configResult = aggregatedConfigWithArgs;
		} else {
			final var configIncoming = configFromPart(defaultsPart, resp, aggregatedConfigWithArgs.schema());
			if (configIncoming == null) {
				configResult = aggregatedConfigWithArgs;
			} else {
				configResult = ConfigUtil.merge(
								aggregatedConfigWithArgs.pathSep(),
								Arrays.asList(aggregatedConfigWithArgs, configIncoming));
			}
		}
		return configResult;
	}

	static Config configFromPart(
					final Part defaultsPart,
					final HttpServletResponse resp,
					final Map<String, Object> configSchema)
					throws IOException, NoSuchMethodException, InvalidValuePathException,
					InvalidValueTypeException {
		final String rawDefaultsData;
		try (final var br = new BufferedReader(new InputStreamReader(defaultsPart.getInputStream()))) {
			rawDefaultsData = br.lines().collect(Collectors.joining("\n"));
		}
		return ConfigUtil.loadConfig(rawDefaultsData, configSchema);
	}

	static String getIncomingScenarioOrDefault(final Part scenarioPart, final Path appHomePath)
					throws IOException {
		final String scenarioResult;
		if (scenarioPart == null) {
			scenarioResult = ScenarioUtil.defaultScenario(appHomePath);
		} else {
			try (final var br = new BufferedReader(new InputStreamReader(scenarioPart.getInputStream()))) {
				scenarioResult = br.lines().collect(Collectors.joining("\n"));
			}
		}
		return scenarioResult;
	}

	@Override
	public final void destroy() {
		scenarioExecutor.close();
		super.destroy();
	}
}
