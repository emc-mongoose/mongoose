package com.emc.mongoose.control.run;

import com.emc.mongoose.concurrent.SingleTaskExecutor;
import com.emc.mongoose.concurrent.SingleTaskExecutorImpl;
import com.emc.mongoose.config.ConfigUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.ScriptEngineUtil;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsManager;
import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;
import org.eclipse.jetty.http.HttpHeader;

import javax.script.ScriptEngine;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 @author veronika K. on 08.11.18 */
public class RunServlet
extends HttpServlet {

	private static final String PART_KEY_DEFAULTS = "defaults";
	private static final String PART_KEY_SCENARIO = "scenario";

	private final ScriptEngine scriptEngine;
	private final List<Extension> extensions;
	private final MetricsManager metricsMgr;
	private final Map<String, Object> configSchema;
	private final SingleTaskExecutor scenarioExecutor = new SingleTaskExecutorImpl();

	public RunServlet(
		final ClassLoader clsLoader, final List<Extension> extensions, final MetricsManager metricsMgr,
		final Map<String, Object> configSchema
	) {
		this.scriptEngine = ScriptEngineUtil.scriptEngineByDefault(clsLoader);
		this.extensions = extensions;
		this.metricsMgr = metricsMgr;
		this.configSchema = configSchema;
	}

	@Override
	protected final void doPost(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException, ServletException {

		final String rawDefaultsData;
		try(
			final InputStream in = req.getPart(PART_KEY_DEFAULTS).getInputStream();
			final BufferedReader br = new BufferedReader(new InputStreamReader(in))
		) {
			rawDefaultsData = br.lines().collect(Collectors.joining("\n"));
		}
		Loggers.CONFIG.info(rawDefaultsData);
		Config defaults =  null;
		try {
			defaults = ConfigUtil.loadConfig(rawDefaultsData, configSchema);
		} catch(final Throwable cause) {
			LogUtil.exception(Level.ERROR, cause, "Failed to parse the defaults");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse the defaults");
		}

		if(defaults != null) {
			final String scenario;
			try(
				final InputStream in = req.getPart(PART_KEY_SCENARIO).getInputStream();
				final BufferedReader br = new BufferedReader(new InputStreamReader(in))
			) {
				scenario = br.lines().collect(Collectors.joining("\n"));
			}

			// expose the received configuration and the step types
			ScriptEngineUtil.configure(scriptEngine, extensions, defaults, metricsMgr);
			//
			final Run run = new RunImpl(defaults.stringVal("run-comment"), scenario, scriptEngine);
			try {
				scenarioExecutor.execute(run);
				resp.setStatus(HttpServletResponse.SC_ACCEPTED);
				setRunTimestampHeader(run, resp);
			} catch(final RejectedExecutionException e) {
				resp.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		}
	}

	@Override
	protected final void doHead(final HttpServletRequest req, final HttpServletResponse resp) {
		applyForActiveRunIfAny(resp, RunServlet::setRunExistsResponse);
	}

	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		extractRequestTimestampAndApply(req, resp, RunServlet::setRunMatchesResponse);
	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		extractRequestTimestampAndApply(req, resp, this::stopRunIfMatchesAndSetResponse);
	}

	static void setRunTimestampHeader(final Run task, final HttpServletResponse resp) {
		resp.setHeader(HttpHeader.ETAG.name(), Long.toString(task.timestamp(), 0x10));
	}

	void applyForActiveRunIfAny(final HttpServletResponse resp, final BiConsumer<Run, HttpServletResponse> action) {
		final Runnable activeTask = scenarioExecutor.task();
		if(null == activeTask) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else if(activeTask instanceof Run) {
			final Run activeRun = (Run) activeTask;
			action.accept(activeRun, resp);
		} else {
			Loggers.ERR.warn("The scenario executor runs an alien task: {}", activeTask);
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	void extractRequestTimestampAndApply(
		final HttpServletRequest req, final HttpServletResponse resp,
		final TriConsumer<Run, HttpServletResponse, Long> runRespTimestampConsumer
	) throws IOException {
		final String reqTimestampRawValue = Collections.list(req.getHeaders(HttpHeader.IF_MATCH.asString()))
			.stream()
			.findAny()
			.orElse(null);
		if(null == reqTimestampRawValue) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing header: " + HttpHeader.IF_MATCH);
		} else {
			try {
				final long reqTimestamp = Long.parseLong(reqTimestampRawValue, 0x10);
				applyForActiveRunIfAny(resp, (run, resp_) -> runRespTimestampConsumer.accept(run, resp_, reqTimestamp));
			} catch(final NumberFormatException e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid start time: " + reqTimestampRawValue);
			}
		}
	}

	static void setRunExistsResponse(final Run run, final HttpServletResponse resp) {
		resp.setStatus(HttpServletResponse.SC_OK);
		setRunTimestampHeader(run, resp);
	}

	static void setRunMatchesResponse(final Run run, final HttpServletResponse resp, final long reqTimestamp) {
		if(run.timestamp() == reqTimestamp) {
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	void stopRunIfMatchesAndSetResponse(final Run run, final HttpServletResponse resp, final long reqTimestamp) {
		if(run.timestamp() == reqTimestamp) {
			scenarioExecutor.stop(run);
			if(null != scenarioExecutor.task()) {
				throw new AssertionError("Run stopping failure");
			}
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Override
	public final void destroy() {
		scenarioExecutor.close();
		super.destroy();
	}
}
