package com.emc.mongoose.webui;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
// mongoose-core-impl.jar
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
// mongoose-server-impl.jar
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
//
import com.emc.mongoose.run.scenario.Chain;
import com.emc.mongoose.run.scenario.Rampup;
import com.emc.mongoose.run.scenario.Single;
//
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//
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
		final String runId = request.getParameter(RunTimeConfig.KEY_RUN_ID);
		if (!isRunIdFree(runId)) {
			try {
				response.getWriter().write("Scenario with this id will " +
					"be interrupted if it's running");
			} catch (final IOException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to write in servlet response");
			}
			return;
		}
		//
		runTimeConfig = (RunTimeConfig) runTimeConfig.clone();
		runTimeConfig.setProperty(RunTimeConfig.KEY_RUN_ID, LogUtil.newRunId());
		setupRunTimeConfig(request);
		updateLastRunTimeConfig(runTimeConfig);
		//
		switch(request.getParameter(RunTimeConfig.KEY_RUN_MODE)) {
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				startServer("Starting the distributed load server");
				break;
			case Constants.RUN_MODE_CINDERELLA:
				startStorageMock("Starting the cinderella");
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
		//
		final Thread thread = new Thread() {
			RunTimeConfig localRunTimeConfig;
			LoadBuilderSvc multiSvc;
			//
			@Override
			public void run() {
				localRunTimeConfig = runTimeConfig;
				RunTimeConfig.setContext(localRunTimeConfig);
				setName("run<" + runTimeConfig.getRunId() + ">");
				//
				LOG.debug(Markers.MSG, message);
				LOG.info(Markers.CFG, runTimeConfig.toFormattedString());
				//
				try {
					multiSvc = new MultiLoadBuilderSvc(localRunTimeConfig);
					multiSvc.start();
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to start the load builder services"
					);
				}
			}
			//
			@Override
			public void interrupt() {
				RunTimeConfig.setContext(localRunTimeConfig);
				try {
					multiSvc.interrupt();
					multiSvc.close();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Networking failure");
				} finally {
					super.interrupt();
				}
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
				setName("run<" + runTimeConfig.getRunId() + ">");
				ThreadContext.put(RunTimeConfig.KEY_SCENARIO_NAME, runTimeConfig.getScenarioName());
				ThreadContext.put(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC,
					String.valueOf(runTimeConfig.getLoadMetricsPeriodSec()));
				//
				final String scenarioName = runTimeConfig.getScenarioName();
				chartsMap.put(runTimeConfig.getRunId(), scenarioName);
				//
				LOG.debug(Markers.MSG, message);
				LOG.info(Markers.CFG, runTimeConfig.toFormattedString());
				//
				switch (scenarioName) {
					case Constants.RUN_SCENARIO_SINGLE:
						new Single(runTimeConfig).run();
						break;
					case Constants.RUN_SCENARIO_CHAIN:
						new Chain(runTimeConfig).run();
						break;
					case Constants.RUN_SCENARIO_RAMPUP:
						ThreadContext.put(RunTimeConfig.KEY_SCENARIO_RAMPUP_SIZES,
							convertArrayToString(runTimeConfig.getScenarioRampupSizes()));
						ThreadContext.put(RunTimeConfig.KEY_SCENARIO_RAMPUP_CONN_COUNTS,
							convertArrayToString(runTimeConfig.getScenarioRampupConnCounts()));
						ThreadContext.put(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD,
							convertArrayToString(runTimeConfig.getScenarioChainLoad()));
						new Rampup(runTimeConfig).run();
						break;
					default:
						throw new IllegalArgumentException(
							String.format("Incorrect scenario: \"%s\"", scenarioName)
						);
				}
				LOG.info(Markers.MSG, "Scenario end");
				//
			}
			//
			@Override
			public void interrupt() {
				LoadExecutor.RESTORED_STATES_MAP.remove(runTimeConfig.getRunId());
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString(RunTimeConfig.KEY_RUN_ID), thread);
	}
	//
	private void startStorageMock(final String message) {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				RunTimeConfig.setContext(runTimeConfig);
				setName("run<" + runTimeConfig.getRunId() + ">");
				//
				LOG.debug(Markers.MSG, message);
				LOG.info(Markers.CFG, runTimeConfig.toFormattedString());
				try {
					new Cinderella(runTimeConfig).run();
				} catch (final IOException e) {
					LogUtil.exception(LOG, Level.FATAL, e, "Failed run Cinderella");
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
		return !threadsMap.containsKey(runId);
	}
}
