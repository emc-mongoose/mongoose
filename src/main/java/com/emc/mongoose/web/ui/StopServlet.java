package com.emc.mongoose.web.ui;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by gusakk on 03/10/14.
 */
public final class StopServlet extends HttpServlet {

	public static Map<String, Boolean> stoppedRunModes;

	@Override
	public void init() throws ServletException {
		super.init();
		stoppedRunModes = new HashMap<>();
	}

	public final void doPost(final HttpServletRequest request, final HttpServletResponse response)
    throws ServletException, IOException {
		StartServlet.interruptMongoose(request.getParameter("run.id"));
		stoppedRunModes.put(request.getParameter("run.id"), true);
		request.getSession(true).setAttribute("stopped", stoppedRunModes);
    }

}
