package com.emc.mongoose.webui;

import com.emc.mongoose.common.io.JsonUtil;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;

import static com.emc.mongoose.webui.MainServlet.PATH_TO_SCENARIO_DIR;

/**
 * Created on 07.04.16.
 */
public class ScenarioServlet extends HttpServlet {

	private static final String REQUEST_PATH_KEY = "path";

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final String relativeScenarioPath =  request.getParameter(REQUEST_PATH_KEY);
		final Path fullScenarioPath =  PATH_TO_SCENARIO_DIR.resolve(relativeScenarioPath);
		final String scenarioJson = JsonUtil.readFileToString(fullScenarioPath);
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		response.getWriter().write(scenarioJson);
	}
}
