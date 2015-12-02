package com.emc.mongoose.webui;

import com.emc.mongoose.common.log.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kirill_gusakov on 30.11.15.
 */
public class StateServlet extends CommonServlet {
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
	@Override
	public void doGet(
		final HttpServletRequest request, final HttpServletResponse response
	) {
		try {
			response.setContentType(ContentType.APPLICATION_JSON.toString());
			final ObjectMapper mapper = new ObjectMapper();
			response.getWriter().write(mapper.writeValueAsString(threadsMap.keySet()));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to write in response");
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
