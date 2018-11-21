package com.emc.mongoose.control;

import com.github.akurilov.confuse.Config;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

/**
 @author veronika K. on 08.11.18 */
public class RunServlet
extends HttpServlet {

	private static final String CONTEXT_SEP = "/";

	@Override
	protected final void doPost(final HttpServletRequest req, final HttpServletResponse resp)
	throws IOException {
		try(final BufferedReader reqPayloadReader = req.getReader()) {
			final Config defaults;
			final String scenario;
			final String
			reqPayloadReader
				.lines()

		}
	}

	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp) {

	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp) {

	}
}
