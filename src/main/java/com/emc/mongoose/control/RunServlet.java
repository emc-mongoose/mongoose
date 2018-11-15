package com.emc.mongoose.control;

import com.emc.mongoose.Node;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

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
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
	throws ServletException, IOException {
		resp.addHeader("Node status", node.status().name());
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		resp.addHeader("Node start time", dtf.format(node.startTime()));
		resp.setStatus(STATUS_OK);
		//TODO: add content
	}
}
