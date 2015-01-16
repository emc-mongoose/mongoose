package com.emc.mongoose.web.ui;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.run.Scenario;
import com.emc.mongoose.run.ThreadContextMap;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.web.load.server.impl.BasicLoadBuilderSvc;
import com.emc.mongoose.web.storagemock.MockServlet;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;

import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.web.ui.enums.RunModes;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	//
	@Override
	public final void init() {
		super.init();
		threadsMap = CommonServlet.THREADS_MAP;
		stoppedRunModes = CommonServlet.STOPPED_RUN_MODES;
	}
	//
	@Override
	public final void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		//
		if (!isRunIdFree(request.getParameter(Main.KEY_RUN_ID))) {
			String resultString;
			if (threadsMap.get(request.getParameter(Main.KEY_RUN_ID)) != null) {
				if (threadsMap.get(request.getParameter(Main.KEY_RUN_ID)).isAlive()) {
					resultString = "Mongoose with this run.id is running at the moment";
				} else {
					resultString = "Tab with the same run.id will be closed";
				}
				try {
					response.getWriter().write(resultString);
				} catch (final IOException e) {
					ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failed to write in servlet response");
				}
			}
			return;
		}
		//
		if (!stoppedRunModes.isEmpty()) {
			stoppedRunModes.remove(request.getParameter(Main.KEY_RUN_ID));
		}
		//
		runTimeConfig = runTimeConfig.clone();
		setupRunTimeConfig(request);
		CommonServlet.updateLastRunTimeConfig(runTimeConfig);
		switch (RunModes.getRunModeConstantByRequest(request.getParameter(Main.KEY_RUN_MODE))) {
			case VALUE_RUN_MODE_SERVER:
				startServer("Starting the server");
				break;
			case VALUE_RUN_MODE_WSMOCK:
				startWSMock("Starting the wsmock");
				break;
			case VALUE_RUN_MODE_CLIENT:
				startStandaloneOrClient("Starting the client");
				break;
			case VALUE_RUN_MODE_STANDALONE:
				startStandaloneOrClient("Starting the standalone");
				break;
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
				Main.RUN_TIME_CONFIG.set(localRunTimeConfig);
				ThreadContextMap.initThreadContextMap();
				//
				LOG.debug(Markers.MSG, message);
				//
				loadBuilderSvc = new BasicLoadBuilderSvc();
				//
				try {
					loadBuilderSvc.setProperties(runTimeConfig);
					loadBuilderSvc.start();
				} catch (final RemoteException e) {
					ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to start load builder service");
				}
			}
			@Override
			public void interrupt() {
				Main.RUN_TIME_CONFIG.set(localRunTimeConfig);
				ThreadContextMap.initThreadContextMap();
				//
				ServiceUtils.close(loadBuilderSvc);
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString(Main.KEY_RUN_ID), thread);
	}
	//
	private void startStandaloneOrClient(final String message) {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap();
				//
				LOG.debug(Markers.MSG, message);
				new Scenario().run();
			}
			//
			@Override
			public void interrupt() {
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString(Main.KEY_RUN_ID), thread);
	}
	//
	private void startWSMock(final String message) {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap();
				//
				LOG.debug(Markers.MSG, message);
				new MockServlet(runTimeConfig).run();
			}

			@Override
			public void interrupt() {
				super.interrupt();
			}
		};

		thread.start();
		threadsMap.put(runTimeConfig.getString(Main.KEY_RUN_ID), thread);
	}
	//
	public final boolean isRunIdFree(final String runId) {
		if (threadsMap.get(runId) != null)
			return false;
		return true;
	}
}
