package com.emc.mongoose.web.ui;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.run.Scenario;
import com.emc.mongoose.run.ThreadContextMap;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadBuilder;
import com.emc.mongoose.web.load.client.WSLoadBuilderClient;
import com.emc.mongoose.web.load.impl.BasicLoadBuilder;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.client.impl.BasicLoadBuilderClient;
import com.emc.mongoose.web.load.client.WSLoadClient;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.web.load.server.impl.BasicLoadBuilderSvc;
import com.emc.mongoose.run.WSMockServlet;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;

import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.web.ui.enums.RunModes;
import org.apache.commons.configuration.ConversionException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 01/10/14.
 */
public final class StartServlet extends HttpServlet {

	private final static Logger LOG = LogManager.getLogger();
	private RunTimeConfig runTimeConfig;
	public static ConcurrentHashMap<String, Thread> threadsMap;

	@Override
	public final void init() throws ServletException {
		super.init();
		runTimeConfig = (RunTimeConfig) getServletContext().getAttribute("runTimeConfig");
		threadsMap = new ConcurrentHashMap<>();
	}
	//
	public void doPost(final HttpServletRequest request, final HttpServletResponse response)
	throws ServletException, IOException {
		//
		runTimeConfig = runTimeConfig.clone();
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
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap(runTimeConfig);
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
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap(runTimeConfig);
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
				ThreadContextMap.initThreadContextMap(Main.RUN_TIME_CONFIG.get());
				//
				LOG.debug(Markers.MSG, message);
				new Scenario(runTimeConfig).run();
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
				ThreadContextMap.initThreadContextMap(runTimeConfig);
				//
				LOG.debug(Markers.MSG, message);
				new WSMockServlet(runTimeConfig).run();
			}

			@Override
			public void interrupt() {
				super.interrupt();
			}
		};

		thread.start();
		threadsMap.put(runTimeConfig.getString("run.id"), thread);
	}
	//
	private void setupRunTimeConfig(final HttpServletRequest request) {
		for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			if (entry.getValue().length > 1) {
				runTimeConfig.set(entry.getKey(), convertArrayToString(entry.getKey(), entry.getValue()));
				continue;
			}
			runTimeConfig.set(entry.getKey(), entry.getValue()[0]);
		}
	}
	//
	private String convertArrayToString(String key, String[] stringArray) {
		String resultString = Arrays.toString(stringArray)
									.replace("[", "")
									.replace("]", "")
									.replace(" ", "")
									.trim();
		// TODO fix it
		if (key.equals("run.time"))
			return resultString.replace(",", ".");
		return resultString;
	}
	//
	public static void interruptMongoose(final String runId) {
		try {
			if (threadsMap.get(runId).isInterrupted()) {
				threadsMap.remove(runId);
			} else {
				threadsMap.get(runId).interrupt();
			}
		} catch (final Exception e) {
			threadsMap.remove(runId);
		}
	}

}
