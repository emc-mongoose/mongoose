package com.emc.mongoose.web.ui;

import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.web.ui.enums.RunModes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by gusakk on 02/10/14.
 */
public final class MainServlet extends HttpServlet {

	private final static Logger LOG = LogManager.getLogger();
	private RunTimeConfig runTimeConfig;

	@Override
	public final void init() throws ServletException {
		runTimeConfig = (RunTimeConfig) getServletContext().getAttribute("runTimeConfig");
		super.init();
	}

	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		request.setAttribute("runmodes", RunModes.values());
		request.setAttribute("runTimeConfig", runTimeConfig);
		if (StartServlet.threadsMap != null) {
			request.getSession(true).setAttribute("runmodes", StartServlet.threadsMap.keySet());
		}
		request.getRequestDispatcher("index.jsp").forward(request, response);
	}

}
