package com.emc.mongoose.webui;

import com.emc.mongoose.common.io.JsonUtil;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Created on 22.04.16.
 */
@MultipartConfig
public class RunServlet extends HttpServlet {

	private static final String APP_CONFIG_KEY = "config";
	private static final String SCENARIO_KEY = "scenario";

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final String appConfig = request.getParameter(APP_CONFIG_KEY);
		final String scenario = request.getParameter(SCENARIO_KEY);
		System.out.println("I got something. So, what now?");
		response.getWriter().write("Mongoose ran");
	}

}
