package com.emc.mongoose.control;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class LogServlet
extends HttpServlet {

	private static final Pattern PATTERN_URI_PATH = Pattern.compile(
		"/logs/(?<stepId>)/(?<loggerName>)"
	);

	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {

	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {

	}
}
