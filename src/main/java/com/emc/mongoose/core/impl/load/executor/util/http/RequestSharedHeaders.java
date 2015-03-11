package com.emc.mongoose.core.impl.load.executor.util.http;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.List;
/**
Created by kurila on 30.01.15.
*/ //
public final class RequestSharedHeaders
implements HttpRequestInterceptor {
	//
	private final List<Header> sharedHeaders;
	//
	public RequestSharedHeaders(final List<Header> sharedHeaders) {
		this.sharedHeaders = sharedHeaders;
	}
	//
	@Override
	public final void process(
		final HttpRequest request, final HttpContext context
	) throws HttpException, IOException {
		for(final Header nextHeader : sharedHeaders) {
			if(!request.containsHeader(nextHeader.getName())) {
				request.setHeader(nextHeader);
			}
		}
	}
}
