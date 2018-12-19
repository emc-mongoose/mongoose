package com.emc.mongoose.control;

import org.eclipse.jetty.rewrite.handler.Rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class AddCorsHeaderRule
extends Rule {

	public static final String HEADER_CORS_NAME = "Access-Control-Allow-Origin";
	public static final String HEADER_CORS_VALUE = "*";

	@Override
	public final String matchAndApply(
		final String target, final HttpServletRequest request, final HttpServletResponse response
	) {
		response.setHeader(HEADER_CORS_NAME, HEADER_CORS_VALUE);
		return null;
	}
}
