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
public class MainServlet extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();
	private RunTimeConfig runTimeConfig;

	@Override
	public void init() throws ServletException {
		runTimeConfig = (RunTimeConfig) getServletContext().getAttribute("runTimeConfig");
		super.init();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		request.setAttribute("runmodes", RunModes.values());
		request.setAttribute("runTimeConfig", runTimeConfig);
		request.getRequestDispatcher("index.jspx").forward(request, response);
	}

}
