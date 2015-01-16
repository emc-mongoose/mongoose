package com.emc.mongoose.web.ui;

import com.emc.mongoose.run.Main;
import com.emc.mongoose.web.ui.logging.WebUIAppender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gusakk on 03/10/14.
 */
public final class StopServlet extends CommonServlet {
	//
	private final static String STOP_TYPE = "type";
	private final static String STOP_REQUEST = "stop";
	private final static String REMOVE_REQUEST = "remove";
	//
	private ConcurrentHashMap<String, Thread> threadsMap;
	private ConcurrentHashMap<String, Boolean> stoppedRunModes;
	//
	@Override
	public void init() {
		super.init();
		threadsMap = CommonServlet.THREADS_MAP;
		stoppedRunModes = CommonServlet.STOPPED_RUN_MODES;
	}
	//
	@Override
	public final void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		final String currentRunId = request.getParameter(Main.KEY_RUN_ID);
		interruptMongoose(currentRunId, request.getParameter(STOP_TYPE));
		stoppedRunModes.put(currentRunId, true);
		request.getSession(true).setAttribute("stopped", stoppedRunModes);
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void interruptMongoose(final String runId, final String type) {
		switch (type) {
			case STOP_REQUEST:
				try {
					threadsMap.get(runId).interrupt();
				} catch (final Exception e) {
					threadsMap.remove(runId);
				}
				break;
			case REMOVE_REQUEST:
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

}
