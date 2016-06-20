package com.emc.mongoose.webui;

import com.emc.mongoose.core.impl.load.tasks.processors.ChartPackage;
import com.emc.mongoose.core.impl.load.tasks.processors.Metric;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.webui.ServletConstants.RUN_ID_KEY;

/**
 * Created on 18.05.16.
 */
public class ChartServlet extends HttpServlet {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	static {
		JSON_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
	}
	private static final String METRIC_NAME_KEY = "metricName";

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final String runId = request.getParameter(RUN_ID_KEY);
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		final Map<String, Map<String, List<Metric>>> charts = ChartPackage.getChart(runId);
		final String logJsonString = JSON_MAPPER.writeValueAsString(charts);
		response.getWriter().write(logJsonString);
	}

}
