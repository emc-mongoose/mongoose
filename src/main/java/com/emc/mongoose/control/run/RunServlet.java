package com.emc.mongoose.control.run;

import com.emc.mongoose.concurrent.SingleTaskExecutor;
import com.emc.mongoose.control.MongooseHeaders;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.ScriptEngineUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsManager;
import static com.emc.mongoose.load.step.Constants.ATTR_CONFIG;

import com.github.akurilov.confuse.Config;

import javax.script.ScriptEngine;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 @author veronika K. on 08.11.18 */
public class RunServlet
extends HttpServlet {

	private static final String CONTEXT_SEP = "/";

	private final ScriptEngine scriptEngine;
	private final List<Extension> extensions;
	private final MetricsManager metricsMgr;
	private final SingleTaskExecutor scenarioExecutor;

	public RunServlet(
		final ClassLoader clsLoader, final List<Extension> extensions, final MetricsManager metricsMgr,
		final SingleTaskExecutor scenarioExecutor
	) {
		this.scriptEngine = ScriptEngineUtil.scriptEngineByDefault(clsLoader);
		this.extensions = extensions;
		this.metricsMgr = metricsMgr;
		this.scenarioExecutor = scenarioExecutor;
	}

	@Override
	protected final void doPost(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		try(final BufferedReader reqPayloadReader = req.getReader()) {
			final Config defaults;
			final String scenario;
			final String comment;
			// TODO read and parse json into the local variables above
			// expose the loaded configuration
			scriptEngine.getContext().setAttribute(ATTR_CONFIG, defaults, ENGINE_SCOPE);
			// expose the step types
			ScriptEngineUtil.registerStepTypes(scriptEngine, extensions, defaults, metricsMgr);
			final Run run = new RunImpl(comment, scenario, scriptEngine);
			try {
				scenarioExecutor.execute(run);
				resp.setStatus(HttpServletResponse.SC_ACCEPTED);
			} catch(final RejectedExecutionException e) {
				resp.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		}
	}

	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp) {

	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		final Runnable activeTask = scenarioExecutor.task();
		if(null == activeTask) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} if(activeTask instanceof Run) {
			final Run activeRun = (Run) activeTask;
			final String reqStartTimeRawValue = req.getHeader(MongooseHeaders.SCENARIO_START_TIME);
			if(null == reqStartTimeRawValue) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				try(final PrintWriter respWriter = resp.getWriter()) {
					respWriter.println("Missing header: " + MongooseHeaders.SCENARIO_START_TIME);
				}
			} else {
				try {
					final long reqStartTime = Long.parseLong(reqStartTimeRawValue);
					if(activeRun.startTimeMillis() == reqStartTime) {
						scenarioExecutor.stop(activeRun);
						resp.setStatus(HttpServletResponse.SC_OK);
					} else {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					}
				} catch(final NumberFormatException e) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					try(final PrintWriter respWriter = resp.getWriter()) {
						respWriter.println("Invalid start time: " + reqStartTimeRawValue);
					}
				}
			}
		} else {
			Loggers.ERR.warn("The scenario executor runs an alien task: {}", activeTask);
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
