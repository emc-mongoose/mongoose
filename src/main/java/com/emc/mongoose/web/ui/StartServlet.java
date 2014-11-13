package com.emc.mongoose.web.ui;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadBuilder;
import com.emc.mongoose.web.load.client.WSLoadBuilderClient;
import com.emc.mongoose.web.load.impl.BasicLoadBuilder;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.client.impl.BasicLoadBuilderClient;
import com.emc.mongoose.web.load.client.WSLoadClient;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.web.load.server.impl.BasicLoadBuilderSvc;
import com.emc.mongoose.run.WSMock;
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
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 01/10/14.
 */
public class StartServlet extends HttpServlet {

	private final static Logger LOG = LogManager.getLogger();
	private RunTimeConfig runTimeConfig;
	public static ConcurrentHashMap<String, Thread> threadsMap;

	@Override
	public void init() throws ServletException {
		super.init();
		runTimeConfig = (RunTimeConfig) getServletContext().getAttribute("runTimeConfig");
		threadsMap = new ConcurrentHashMap<>();
	}
	//
	public void doPost(final HttpServletRequest request, final HttpServletResponse response)
	throws ServletException, IOException {
		//
		switch (RunModes.valueOf(request.getParameter("runmode"))) {
			case VALUE_RUN_MODE_STANDALONE:
				runTimeConfig = runTimeConfig.clone();
				setupRunTimeConfig(request);
				runStandalone();
				break;
			case VALUE_RUN_MODE_CLIENT:
				runTimeConfig = runTimeConfig.clone();
				setupRunTimeConfig(request);
				runClient();
				break;
			case VALUE_RUN_MODE_SERVER:
				runTimeConfig = runTimeConfig.clone();
				runServer();
				break;
			case VALUE_RUN_MODE_WSMOCK:
				runTimeConfig = runTimeConfig.clone();
				runWSMock();
				break;
			default:
				runTimeConfig = runTimeConfig.clone();
				setupRunTimeConfig(request);
				runStandalone();
				break;
		}

		//	Add runModes to the http session
		request.getSession(true).setAttribute("runmodes", threadsMap.keySet());
	}
	//
	private void runServer() {
		final Thread thread = new Thread() {
			WSLoadBuilderSvc loadBuilderSvc;
			@Override
			public void run() {
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				LOG.debug(Markers.MSG, "Starting the server");
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
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				ServiceUtils.close(loadBuilderSvc);
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString("run.id"), thread);
	}
	//
	private void runClient() {
		//
		final Thread thread = new Thread() {
			WSLoadClient<WSObject> loadClient;
			@Override
			public void run() {
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				LOG.debug(Markers.MSG, "Starting the client");
				//
				try {
					final WSLoadBuilderClient<WSObject, WSLoadClient<WSObject>> loadBuilderClient = new BasicLoadBuilderClient<>(runTimeConfig);
					//
					try {
						final Request.Type loadType = Request.Type.valueOf(
								runTimeConfig.getString("scenario.single.load").toUpperCase()
						);
						loadBuilderClient.setLoadType(loadType);
					} catch (final NoSuchElementException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "No load type specified, try arg -Dscenario.single.load=<VALUE> to override");
					} catch (final IllegalArgumentException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "No such load type, it should be a constant from Load.Type enumeration");
					}
					//
					//final WSLoadExecutor loadExecutor = loadBuilder.build();
					loadClient = loadBuilderClient.build();
					//
					final String timeOutString;
					final String[] timeOutArray;
					//
					try {
						timeOutString = runTimeConfig.getString("run.time");
						timeOutArray = timeOutString.split("\\.");
					} catch (final NoSuchElementException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override");
						return;
					} catch (final IllegalArgumentException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "Timeout unit should be a name of a constant from TimeUnit enumeration");
						return;
					} catch (final IllegalStateException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "Time unit should be specified with timeout value (following after \".\" separator)");
						return;
					}
					//
					loadClient.start();
					//
					try {
						loadClient.join(TimeUnit.valueOf(timeOutArray[1].toUpperCase()).toMillis(Integer.valueOf(timeOutArray[0])));
					} catch (final InterruptedException e) {
						ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted");
					}
					//
					LOG.info(Markers.MSG, "Scenario end");
					loadClient.close();
				} catch (final ConversionException e) {
					ExceptionHandler.trace(LOG, Level.FATAL, e, "Servers address list should be comma delimited");
				} catch (final NoSuchElementException e) {
					ExceptionHandler.trace(LOG, Level.FATAL, e, "Servers address list not specified, try  arg -Dremote.servers=<LIST> to override");
				} catch (final IOException e) {
					ExceptionHandler.trace(LOG, Level.FATAL, e, "Failed to create load builder client");
				}
			}

			@Override
			public void interrupt() {
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				try {
					if (loadClient != null) {
						loadClient.close();
					}
				} catch (final IOException e) {
					ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to start client mode");
				}
				super.interrupt();
			}
		};
		thread.start();
		threadsMap.put(runTimeConfig.getString("run.id"), thread);

	}
	//
	private void runStandalone()
	throws IOException {
		final Thread thread = new Thread() {
			WSLoadExecutor<WSObject> loadExecutor;
			@Override
			public void run() {
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				LOG.debug(Markers.MSG, "Starting the standalone");
				try {
					//
					final WSLoadBuilder<WSObject, WSLoadExecutor<WSObject>> loadBuilder = new BasicLoadBuilder<>();
					loadBuilder.setProperties(runTimeConfig);
					//
					try {
						final Request.Type loadType = Request.Type.valueOf(
								runTimeConfig.getString("scenario.single.load").toUpperCase()
						);
						loadBuilder.setLoadType(loadType);
					} catch (final NoSuchElementException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "No load type specified, try arg -Dscenario.single.load=<VALUE> to override");
					} catch (final IllegalArgumentException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "No such load type, it should be a constant from Load.Type enumeration");
					}
					//
					//final WSLoadExecutor loadExecutor = loadBuilder.build();
					loadExecutor = loadBuilder.build();
					//
					final String timeOutString;
					final String[] timeOutArray;
					//
					try {
						timeOutString = runTimeConfig.getString("run.time");
						timeOutArray = timeOutString.split("\\.");
					} catch (final NoSuchElementException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override");
						return;
					} catch (final IllegalArgumentException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "Timeout unit should be a name of a constant from TimeUnit enumeration");
						return;
					} catch (final IllegalStateException e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e, "Time unit should be specified with timeout value (following after \".\" separator)");
						return;
					}
					//
					loadExecutor.start();
					//
					try {
						loadExecutor.join(TimeUnit.valueOf(timeOutArray[1].toUpperCase()).toMillis(Integer.valueOf(timeOutArray[0])));
					} catch (final InterruptedException e) {
						ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted");
					}
					//
					LOG.info(Markers.MSG, "Scenario end");
					loadExecutor.close();
				} catch (final IOException e) {
					ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to start standalone mode");
				}
			}

			@Override
			public void interrupt() {
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				try {
				   loadExecutor.close();
				} catch (IOException e) {
				   ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to close the load executor");
				}
				//
				super.interrupt();
			}
		};

		thread.start();
		threadsMap.put(runTimeConfig.getString("run.id"), thread);
	}
	//
	private void runWSMock() {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				ThreadContext.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
				//
				LOG.debug(Markers.MSG, "Starting the wsmock");
				new WSMock(runTimeConfig).run();
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
		//	Common settings
		runTimeConfig.set("run.time", request.getParameter("runTime") + "." + request.getParameter("runTimeSelect"));
		runTimeConfig.set("run.metrics.period.sec", request.getParameter("runMetricsPeriodSec"));
		runTimeConfig.set("auth.id", request.getParameter("authId"));
		runTimeConfig.set("auth.secret", request.getParameter("authSecret"));
		//	Data & Load
		runTimeConfig.set("data.count", request.getParameter("dataCount"));
		runTimeConfig.set("data.size.min", request.getParameter("dataSizeMin"));
		runTimeConfig.set("data.size.max", request.getParameter("dataSizeMax"));
		//
		runTimeConfig.set("load.create.threads", request.getParameter("loadCreateThreads"));
		//
		runTimeConfig.set("load.read.threads", request.getParameter("loadReadThreads"));
		runTimeConfig.set("load.read.verify.content", request.getParameter("loadReadVerifyContent"));
		//
		runTimeConfig.set("load.update.threads", request.getParameter("loadUpdateThreads"));
		runTimeConfig.set("load.update.per.item", request.getParameter("loadUpdatePerItem"));
		//
		runTimeConfig.set("load.delete.threads", request.getParameter("loadDeleteThreads"));
		//
		runTimeConfig.set("load.append.threads", request.getParameter("loadAppendThreads"));
		//	API
		runTimeConfig.set("api.s3.port", request.getParameter("apiS3Port"));
		runTimeConfig.set("api.s3.auth.prefix", request.getParameter("apiS3AuthPrefix"));
		runTimeConfig.set("api.s3.bucket", request.getParameter("apiS3Bucket"));
		//
		runTimeConfig.set("api.atmos.port", request.getParameter("apiAtmosPort"));
		runTimeConfig.set("api.atmos.subtenant", request.getParameter("apiAtmosSubtenant"));
		runTimeConfig.set("api.atmos.path.rest", request.getParameter("apiAtmosPathRest"));
		runTimeConfig.set("api.atmos.interface", request.getParameter("apiAtmosInterface"));
		//
		runTimeConfig.set("api.swift.port", request.getParameter("apiSwiftPort"));
		//	Storage
		runTimeConfig.set("storage.api", request.getParameter("storageApi"));
		runTimeConfig.set("storage.scheme", request.getParameter("scheme"));
		runTimeConfig.set("storage.addrs", Arrays.toString(request.getParameterValues("dataNodes"))
				.replace("[", "")
				.replace("]", "")
				.trim());
		//	Drivers
		runTimeConfig.set("remote.monitor.port", request.getParameter("remoteMonitorPort"));
		runTimeConfig.set("remote.servers", Arrays.toString(request.getParameterValues("drivers"))
				.replace("[", "")
				.replace("]", "")
				.trim());
		//	Scenario
		runTimeConfig.set("scenario.single.load", request.getParameter("scenarioSingleLoad"));

	}
	//
	public static void interruptMongoose(final String runId) {
		threadsMap.get(runId).interrupt();
		threadsMap.remove(runId);
	}

}
