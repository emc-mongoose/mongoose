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
				resp.setHeader(HttpHeader.ETAG.name(), Long.toString(run.timestamp(), 0x10));
			} catch(final RejectedExecutionException e) {
				resp.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		}
	}

	@Override
	protected final void doHead(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		final Runnable activeTask = scenarioExecutor.task();
		if(null == activeTask) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else if(activeTask instanceof Run) {
			final Run activeRun = (Run) activeTask;
			final String reqTimestampRawValue = Collections.list(req.getHeaderNames())
				.stream()
				.map(String::toLowerCase)
				.filter(HttpHeader.IF_MATCH.asString()::equalsIgnoreCase)
				.findAny()
				.map(req::getHeader)
				.orElse(null);
			if(null == reqTimestampRawValue) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing header: " + HttpHeader.IF_MATCH);
			} else {
				try {
					final long reqTimestamp = Long.parseLong(reqTimestampRawValue, 0x10);
					if(activeRun.timestamp() == reqTimestamp) {
						resp.setStatus(HttpServletResponse.SC_OK);
					} else {
						resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
					}
				} catch(final NumberFormatException e) {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid start time: " + reqTimestampRawValue);
				}
			}
		} else {
			Loggers.ERR.warn("The scenario executor runs an alien task: {}", activeTask);
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		final Runnable activeTask = scenarioExecutor.task();
		if(null == activeTask) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else if(activeTask instanceof Run) {
			final Run activeRun = (Run) activeTask;
			final String reqTimestampRawValue = Collections.list(req.getHeaderNames())
				.stream()
				.map(String::toLowerCase)
				.filter(HttpHeader.IF_MATCH.asString()::equalsIgnoreCase)
				.findAny()
				.map(req::getHeader)
				.orElse(null);
			if(null == reqTimestampRawValue) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing header: " + HttpHeader.IF_MATCH);
			} else {
				try {
					final long reqTimestamp = Long.parseLong(reqTimestampRawValue, 0x10);
					if(activeRun.timestamp() == reqTimestamp) {
						scenarioExecutor.stop(activeRun);
						if(null != scenarioExecutor.task()) {
							throw new AssertionError("Run stopping failure");
						}
						resp.setStatus(HttpServletResponse.SC_OK);
					} else {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					}
				} catch(final NumberFormatException e) {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid start time: " + reqTimestampRawValue);
				}
			}
		} else {
			Loggers.ERR.warn("The scenario executor runs an alien task: {}", activeTask);
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Override
	public final void destroy() {
		scenarioExecutor.close();
		super.destroy();
	}
}
