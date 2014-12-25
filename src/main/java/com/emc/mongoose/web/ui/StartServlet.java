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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Created by gusakk on 01/10/14.
 */
public final class StartServlet extends HttpServlet {

	private final static Logger LOG = LogManager.getLogger();
	private RunTimeConfig runTimeConfig;
	public static ConcurrentHashMap<String, Thread> threadsMap;
	public static RunTimeConfig LAST_RUN_TIME_CONFIG;

	@Override
	public final void init() throws ServletException {
		super.init();
		runTimeConfig = (RunTimeConfig) getServletContext().getAttribute("runTimeConfig");
		threadsMap = new ConcurrentHashMap<>();
	}
	//
	public void doPost(final HttpServletRequest request, final HttpServletResponse response)
	throws ServletException, IOException {
		if (!isRunIdFree(request.getParameter(RunTimeConfig.KEY_RUN_ID))) {
			String resultString;
			if (threadsMap.get(request.getParameter(RunTimeConfig.KEY_RUN_ID)) != null) {
				if (threadsMap.get(request.getParameter(RunTimeConfig.KEY_RUN_ID)).isAlive()) {
					resultString = "Mongoose with this run.id is running at the moment";
				} else {
					resultString = "Logs with the previous run.mode in the same run.id will be mixed";
				}
				response.getWriter().write(resultString);
			}
			return;
		}
		//
		if (StopServlet.stoppedRunModes != null) {
			StopServlet.stoppedRunModes.remove(request.getParameter(RunTimeConfig.KEY_RUN_ID));
		}
		//
		runTimeConfig = runTimeConfig.clone();
		//
		LAST_RUN_TIME_CONFIG = runTimeConfig;
		//
		setupRunTimeConfig(request);
		//
		switch (RunModes.getRunModeConstantByRequest(request.getParameter("run.mode"))) {
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
		//	Add runModes to the http session
		request.getSession(true).setAttribute("runmodes", threadsMap.keySet());
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
		threadsMap.put(runTimeConfig.getString("run.id"), thread);
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
		threadsMap.put(runTimeConfig.getString("run.id"), thread);
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
		threadsMap.put(runTimeConfig.getString("run.id"), thread);
	}

	public boolean isRunIdFree(final String runId) {
		if (threadsMap.get(runId) != null)
			return false;
		return true;
	}
	//
	private void setupRunTimeConfig(final HttpServletRequest request) {
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			if (entry.getValue()[0].trim().isEmpty()) {
				continue;
			}
			if (entry.getValue().length > 1) {
				runTimeConfig.set(entry.getKey(), convertArrayToString(entry.getKey(), entry.getValue()));
				continue;
			}
			runTimeConfig.set(entry.getKey(), entry.getValue()[0].trim());
		}
	}
	//
	private String convertArrayToString(final String key, final String[] stringArray) {
		final String resultString = Arrays.toString(stringArray)
									.replace("[", "")
									.replace("]", "")
									.replace(" ", "")
									.trim();
		if (key.equals("run.time"))
			return resultString.replace(",", ".");
		return resultString;
	}
	//
	public static void interruptMongoose(final String runId, final String type) {
		switch (type) {
			case "stop":
				try {
					threadsMap.get(runId).interrupt();
				} catch (final Exception e) {
					threadsMap.remove(runId);
				}
				break;
			case "remove":
				try {
					threadsMap.get(runId).interrupt();
					threadsMap.remove(runId);
				} catch (final Exception e) {
					threadsMap.remove(runId);
				}
				break;
		}
	}

}
