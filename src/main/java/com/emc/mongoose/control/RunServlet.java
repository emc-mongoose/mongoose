package com.emc.mongoose.control;

import com.emc.mongoose.Node;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 @author veronika K. on 08.11.18 */
public class RunServlet
	extends HttpServlet {

	private static final String CONTEXT_SEP = "/";
	private static final int STATUS_OK = 200;
	private static final int STATUS_ERROR = 400;
	private final Node node;

	public RunServlet(final Node node) {
		this.node = node;
	}

	@Override
	protected void doHead(final HttpServletRequest req, final HttpServletResponse resp)
	throws ServletException, IOException {
		resp.addHeader("Status of node", node.status().toString());
		resp.setHeader("Status of node", node.status().toString());
		resp.setStatus(STATUS_OK);
		resp.getWriter().print("STATUS: " + node.status());
		resp.getWriter().print("START TIME: " + node.startTime());
		resp.getWriter().print("\nHEAD\n");
		System.out.println("\n\nHEAD\n\n");
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
	throws ServletException, IOException {
		super.doGet(req, resp);
	}
}
