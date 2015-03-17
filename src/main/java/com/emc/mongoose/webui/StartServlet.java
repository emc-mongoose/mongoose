package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.logging.TraceLogger;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
//
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
//
import com.emc.mongoose.storage.mock.impl.cinderella.Main;
//
import com.emc.mongoose.run.scenario.RunTask;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gusakk on 01/10/14.
 */
public final class StartServlet extends CommonServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String RUN_MODES = "runmodes";
	//
	private ConcurrentHashMap<String, Thread> threadsMap;
	private ConcurrentHashMap<String, Boolean> stoppedRunModes;
	private ConcurrentHashMap<String, String> chartsMap;
	//
	@Override
	public final void init() {
		super.init();
		threadsMap = THREADS_MAP;
		stoppedRunModes = STOPPED_RUN_MODES;
		chartsMap = CHARTS_MAP;
	}
	//
	@Override
	public final void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		//
		if (!isRunIdFree(request.getParameter(RunTimeConfig.KEY_RUN_ID))) {
			String resultString;
			if (threadsMap.get(request.getParameter(RunTimeConfig.KEY_RUN_ID)) != null) {
				if (threadsMap.get(request.getParameter(RunTimeConfig.KEY_RUN_ID)).isAlive()) {
					resultString = "Mongoose with this run.id is running at the moment";
				} else {
					resultString = "Tab with the same run.id will be closed";
				}
				try {
					response.getWriter().write(resultString);
				} catch (final IOException e) {
					TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to write in servlet response");
				}
			}
			return;
		}
		//
		if (!stoppedRunModes.isEmpty()) {
			stoppedRunModes.remove(request.getParameter(RunTimeConfig.KEY_RUN_ID));
		}
		//
		runTimeConfig = runTimeConfig.clone();
		setupRunTimeConfig(request);
		updateLastRunTimeConfig(runTimeConfig);
		switch(request.getParameter(RunTimeConfig.KEY_RUN_MODE)) {
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				startServer("Starting the distributed load server");
				break;
			case Constants.RUN_MODE_CINDERELLA:
				startCinderella("Starting the cinderella");
				break;
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				startStandaloneOrClient("Starting the distributed load client");
				break;
			case Constants.RUN_MODE_STANDALONE:
				startStandaloneOrClient("Starting in the standalone mode");
				break;
			default:
				LOG.warn(
					Markers.ERR, "Unsupported run mode \"{}\"",
					request.getParameter(RunTimeConfig.KEY_RUN_MODE)
				);
		}
		//  Add runModes to http session
		request.getSession(true).setAttribute(RUN_MODES, threadsMap.keySet());
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void startServer(final String message) {
		final Thread thread = new Thread() {
			WSLoadBuilderSvc loadBuilderSvc;
			RunTimeConfig localRunTimeConfig;
			@Override
			 public void run() {
				localRunTimeConfig = runTimeConfig;
				RunTimeConfig.setContext(localRunTimeConfig);
				//
				LOG.debug(Markers.MSG, message);
				//
				loadBuilderSvc = new BasicWSLoadBuilderSvc(localRunTimeConfig);
				//
				try {
					loadBuilderSvc.setProperties(runTimeConfig);
					loadBuilderSvc.start();
				} catch (final RemoteException e) {
					TraceLogger.failure(LOG, Level.ERROR, e, "Failed to start load builder service");
				}
			}
			@Override
			public void interrupt() {
				RunTimeConfig.setContext(localRunTimeConfig);
				//
				ServiceUtils.close(loadBuilderSvc);
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString(RunTimeConfig.KEY_RUN_ID), thread);
	}
	//
	private void startStandaloneOrClient(final String message) {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				RunTimeConfig.setContext(runTimeConfig);
				ThreadContext.put(RunTimeConfig.KEY_RUN_SCENARIO_NAME, runTimeConfig.getRunScenarioName());
				ThreadContext.put(
					RunTimeConfig.KEY_RUN_METRICS_PERIOD_SEC,
					String.valueOf(runTimeConfig.getRunMetricsPeriodSec())
				);
				if (runTimeConfig.getRunScenarioName().equals("rampup")) {
					ThreadContext.put("scenario.rampup.sizes", runTimeConfig.getProperty("scenario.rampup.sizes").toString());
					ThreadContext.put("scenario.rampup.thread.counts",
						runTimeConfig.getProperty("scenario.rampup.thread.counts").toString());
					ThreadContext.put("scenario.chain.load", runTimeConfig.getProperty("scenario.chain.load").toString());
				}
				chartsMap.put(runTimeConfig.getRunId(), runTimeConfig.getRunScenarioName());
				//
				LOG.debug(Markers.MSG, message);
				new RunTask().run();
			}
			//
			@Override
			public void interrupt() {
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString(RunTimeConfig.KEY_RUN_ID), thread);
	}
	//
	private void startCinderella(final String message) {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				RunTimeConfig.setContext(runTimeConfig);
				//
				LOG.debug(Markers.MSG, message);
				try {
					new Main(runTimeConfig).run();
				} catch (final IOException e) {
					TraceLogger.failure(LOG, Level.FATAL, e, "Failed run Cinderella");
				}
			}

			@Override
			public void interrupt() {
				super.interrupt();
			}
		};

		thread.start();
		threadsMap.put(runTimeConfig.getString(RunTimeConfig.KEY_RUN_ID), thread);
	}
	//
	public final boolean isRunIdFree(final String runId) {
		if (threadsMap.get(runId) != null)
			return false;
		return true;
	}
}
