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
		switch (RunModes.valueOf(request.getParameter("runmode"))) {
			case VALUE_RUN_MODE_SERVER:
				runTimeConfig = runTimeConfig.clone();
				//
				setupRunTimeConfig(request);
				runServer();
				break;
			case VALUE_RUN_MODE_WSMOCK:
				runTimeConfig = runTimeConfig.clone();
				//
				setupRunTimeConfig(request);
				runWSMock();
				break;
			case VALUE_RUN_MODE_CLIENT:
				runTimeConfig = runTimeConfig.clone();
				//
				setupRunTimeConfig(request);
				runClient();
				break;
			case VALUE_RUN_MODE_STANDALONE:
				runTimeConfig = runTimeConfig.clone();
				//
				setupRunTimeConfig(request);
				runStandalone();
				break;
		}
		//	Add runModes to the http session
		request.getSession(true).setAttribute("runmodes", threadsMap.keySet());
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void runServer() {
		final Thread thread = new Thread() {
			WSLoadBuilderSvc loadBuilderSvc;
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap(runTimeConfig);
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
	private void runStandalone() {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap(Main.RUN_TIME_CONFIG.get());
				//
				LOG.debug(Markers.MSG, "Starting the standalone");
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
	private void runClient() {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap(Main.RUN_TIME_CONFIG.get());
				//
				LOG.debug(Markers.MSG, "Starting the client");
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
	private void runWSMock() {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				Main.RUN_TIME_CONFIG.set(runTimeConfig);
				ThreadContextMap.initThreadContextMap(runTimeConfig);
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
		runTimeConfig.set(Main.KEY_RUN_MODE, RunModes.valueOf(request.getParameter("runmode")).getValue());
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
		runTimeConfig.set("remote.export.port", request.getParameter("remoteExportPort"));
		runTimeConfig.set("remote.import.port", request.getParameter("remoteImportPort"));
		runTimeConfig.set("remote.servers", Arrays.toString(request.getParameterValues("drivers"))
				.replace("[", "")
				.replace("]", "")
				.trim());
		//	Scenario
		runTimeConfig.set("run.scenario.name", request.getParameter("runScenarioName"));
		//	Single
		runTimeConfig.set("scenario.single.load", request.getParameter("scenarioSingleLoad"));
		//	Chain
		runTimeConfig.set("scenario.chain.load", request.getParameter("scenarioChainLoad"));
		runTimeConfig.set("scenario.chain.simultaneous", request.getParameter("scenarioChainSimultaneous"));
		//	Rampup
		runTimeConfig.set("scenario.rampup.thread.counts", request.getParameter("scenarioRampupThreadCounts"));
		runTimeConfig.set("scenario.rampup.sizes", request.getParameter("scenarioRampupSizes"));
		//	Rampup-Create
		runTimeConfig.set("scenario.rampup-create.load", request.getParameter("scenarioRampupCreateLoad"));
		runTimeConfig.set("scenario.rampup-create.objectsizes", request.getParameter("scenarioRampupCreateObjectSizes"));
		runTimeConfig.set("scenario.rampup-create.threads", request.getParameter("scenarioRampupCreateThreads"));
	}
	//
	public static void interruptMongoose(final String runId) {
		try {
			threadsMap.get(runId).interrupt();
			threadsMap.remove(runId);
		} catch (final Exception e) {
			threadsMap.remove(runId);
		}
	}

}
