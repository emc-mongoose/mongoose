package com.emc.mongoose.webui;

import com.emc.mongoose.common.log.appenders.ShortenedLogEvent;
import com.emc.mongoose.common.log.appenders.WebUIAppender;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
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
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.webui.ServletConstants.RUN_ID_KEY;

public final class LogServlet extends HttpServlet {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	static {
		JSON_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
	}
	private static final String MARKER_NAME_KEY = "markerName";
	private static final String TIME_STAMP_KEY = "timeStamp";

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final String runId = request.getParameter(RUN_ID_KEY);
		final String markerName = request.getParameter(MARKER_NAME_KEY);
		final long timeStamp = Long.valueOf(request.getParameter(TIME_STAMP_KEY));
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		final Map<String, List<ShortenedLogEvent>>
			lastLogEvents = WebUIAppender.getLastLogEventsByMarker(runId, markerName, timeStamp);
		final String logJsonString = JSON_MAPPER.writeValueAsString(lastLogEvents);
		response.getWriter().write(logJsonString);
	}
}
