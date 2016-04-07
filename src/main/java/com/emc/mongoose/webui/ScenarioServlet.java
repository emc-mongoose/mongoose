package com.emc.mongoose.webui;

import com.emc.mongoose.common.io.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created on 07.04.16.
 */
public class ScenarioServlet extends HttpServlet {

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("I got something");
	}
}
