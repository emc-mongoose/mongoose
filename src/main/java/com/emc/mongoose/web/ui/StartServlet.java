package com.emc.mongoose.web.ui;
//
import com.emc.mongoose.run.JettyRunner;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.run.Scenario;
import com.emc.mongoose.run.ThreadContextMap;
import com.emc.mongoose.util.conf.DirectoryLoader;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.web.load.server.impl.BasicLoadBuilderSvc;
import com.emc.mongoose.web.storagemock.MockServlet;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;

import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.web.ui.enums.RunModes;
import com.emc.mongoose.web.ui.logging.WebUIAppender;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.SendingContext.RunTime;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;
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
		if (!isRunIdFree(request.getParameter(Main.KEY_RUN_ID))) {
			String resultString;
			if (threadsMap.get(request.getParameter(Main.KEY_RUN_ID)) != null) {
				if (threadsMap.get(request.getParameter(Main.KEY_RUN_ID)).isAlive()) {
					resultString = "Mongoose with this run.id is running at the moment";
				} else {
					resultString = "Tab with the same run.id will be closed";
				}
				response.getWriter().write(resultString);
			}
			return;
		}
		//
		if (StopServlet.stoppedRunModes != null) {
			StopServlet.stoppedRunModes.remove(request.getParameter(Main.KEY_RUN_ID));
		}
		//
		runTimeConfig = runTimeConfig.clone();
		//
		setupRunTimeConfig(request);
		LAST_RUN_TIME_CONFIG = runTimeConfig;
		//
		//DirectoryLoader.loadPropsToDirsFromRunTimeConfig(Paths.get(Main.DIR_ROOT, Main.DIR_CONF, Main.DIR_PROPERTIES), runTimeConfig);
		//File file = Paths.get(Main.DIR_ROOT, JettyRunner.DIR_WEBAPP, JettyRunner.DIR_CONF).toFile();
		//saveCurrentConfigToFile(runTimeConfig);
		//readFromConfigFile(runTimeConfig, file);
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
					//
					WebUIAppender.removeRunId(runId);
				} catch (final Exception e) {
					threadsMap.remove(runId);
				}
				break;
		}
	}
	//
	/*private void saveCurrentConfigToFile(final RunTimeConfig runTimeConfig) {
		//
		final File file = Paths.get(Main.DIR_ROOT, JettyRunner.DIR_WEBAPP,
				JettyRunner.DIR_CONF).toFile();
		if (!file.mkdirs()) {
			if (!file.exists()) {
				LOG.warn(Markers.ERR, "Can't create folders for ui config");
			}
		}
		try {
			final FileOutputStream outputStream = new FileOutputStream(file.toString() + File.separator + "config");
			final Properties props = new Properties();
			final Set<String> keys = runTimeConfig.getUserKeys();
			for (String key : keys) {
				Object value = runTimeConfig.getProperty(key);
				if (List.class.isInstance(value)) {
					props.setProperty(key, StringUtils.join(List.class.cast(value), RunTimeConfig.LIST_SEP));
				} else if (String.class.isInstance(value)) {
					props.setProperty(key, String.class.cast(value));
				}
			}
			try {
				props.store(outputStream, null);
			} catch (final IOException e) {
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Can't store properties in config file");
			}
		} catch (final FileNotFoundException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "File for saving configuration not found");
		}
	}

	private void readFromConfigFile(final RunTimeConfig runTimeConfig, final File file) {
		final Properties props = new Properties();
		final Set<String> keys = runTimeConfig.getUserKeys();
		try {
			final FileInputStream inputStream = new FileInputStream(file + File.separator + "config");
			props.load(inputStream);
			for (String key : keys) {
				runTimeConfig.setProperty(key, props.getProperty(key));
			}
		} catch (final FileNotFoundException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "File for reading configuration not found");
		} catch (final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Can't read properties from file");
		}
	}*/

}
