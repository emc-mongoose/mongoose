package com.emc.mongoose.webui;

import com.emc.mongoose.common.log.appenders.WebUIAppender;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class LogServlet extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	private static final String RUN_ID_KEY = "runId";
	private static final String MARKER_NAME_KEY = "markerName";
	private static final String TIME_STAMP_KEY = "timeStamp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String runId = request.getParameter(RUN_ID_KEY);
		final String markerName = request.getParameter(MARKER_NAME_KEY);
		final String timeStamp = request.getParameter(TIME_STAMP_KEY);
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		response.getWriter().write(JSON_MAPPER.writeValueAsString(WebUIAppender.LOG_EVENTS_MAP));
	}
}
