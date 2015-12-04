package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.appenders.WebUIAppender;
//
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;
//
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Created by gusakk on 03/10/14.
 */
public final class StopServlet extends CommonServlet {
	//
	private final static String STOP_TYPE = "type";
	private final static String TYPE_REMOVE = "remove";
	//
	private final static Logger LOG = LogManager.getLogger();
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
	public final void doGet(
		final HttpServletRequest request, final HttpServletResponse response
	) {
		try {
			response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
			final ObjectMapper mapper = new ObjectMapper();
			response.getWriter().write(mapper.writeValueAsString(stoppedRunModes));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to write in response");
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	@Override
	public final void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		final String currentRunId = request.getParameter(RunTimeConfig.KEY_RUN_ID);
		try {
			if (request.getParameter(STOP_TYPE).equals(TYPE_REMOVE)) {
				stopMongoose(currentRunId, true);
				if (stoppedRunModes.containsKey(currentRunId)) {
					stoppedRunModes.remove(currentRunId);
				}
			} else {
				stopMongoose(currentRunId);
				stoppedRunModes.put(currentRunId, true);
			}
		} catch (InterruptedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Mongoose can't stop correctly through Web UI");
		}
		request.getSession(true).setAttribute("stopped", stoppedRunModes);
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void stopMongoose(final String runId)
	throws InterruptedException {
		stopMongoose(runId, false);
	}
	//
	private void stopMongoose(final String runId, final boolean removeTab)
	throws InterruptedException {
		final Thread runnerThread = threadsMap.get(runId);
		if (runnerThread != null) {
			if (runnerThread.isAlive()) {
				runnerThread.interrupt();
				runnerThread.join();
			}
			if (removeTab) {
				threadsMap.remove(runId);
				WebUIAppender.removeRunId(runId);
			}
		}
	}
}
