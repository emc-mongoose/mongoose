package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.webui.logging.WebUIAppender;
//
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//
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
		threadsMap = THREADS_MAP;
		stoppedRunModes = STOPPED_RUN_MODES;
	}
	//
	@Override
	public final void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		final String currentRunId = request.getParameter(RunTimeConfig.KEY_RUN_ID);
		switch(request.getParameter(STOP_TYPE)) {
			case STOP_REQUEST:
				stopMongoose(currentRunId);
				break;
			case REMOVE_REQUEST:
				stopMongoose(currentRunId);
				WebUIAppender.removeRunId(currentRunId);
				break;
		}
		stoppedRunModes.put(currentRunId, true);
		request.getSession(true).setAttribute("stopped", stoppedRunModes);
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void stopMongoose(final String runId) {
		final Thread runnerThread = threadsMap.get(runId);
		if(runnerThread != null) {
			if(runnerThread.isInterrupted()) {
				if(threadsMap.containsKey(runId)) {
					threadsMap.remove(runId);
				}
			} else {
				runnerThread.interrupt();
				try {
					runnerThread.join();
				} catch(final InterruptedException ignore) {
				}
			}
		}
	}
}
