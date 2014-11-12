package com.emc.mongoose.web.ui;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by gusakk on 03/10/14.
 */
public final class StopServlet extends HttpServlet {

	public final void doPost(final HttpServletRequest request, final HttpServletResponse response)
    throws ServletException, IOException {
		StartServlet.interruptMongoose(request.getParameter("runid"));
    }

}
