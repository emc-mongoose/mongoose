package com.emc.mongoose.web.load.impl.reqproc;
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
public final class SharedHeaders
implements HttpRequestInterceptor {
	//
	private final List<Header> sharedHeaders;
	//
	public SharedHeaders(final List<Header> sharedHeaders) {
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
