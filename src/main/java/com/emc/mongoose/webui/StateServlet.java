package com.emc.mongoose.webui;

import com.emc.mongoose.common.log.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by kirill_gusakov on 30.11.15.
 */
public class StateServlet extends CommonServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String RUN_MODES = "runmodes";
	//
	private Map<String, Thread> threadsMap;
	private Map<String, Boolean> stoppedRunModes;
	private Map<String, String> chartsMap;
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
			response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
			final ObjectMapper mapper = new ObjectMapper();
			response.getWriter().write(mapper.writeValueAsString(threadsMap.keySet()));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to write in response");
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
