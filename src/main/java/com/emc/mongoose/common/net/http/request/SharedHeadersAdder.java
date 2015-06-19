package com.emc.mongoose.common.net.http.request;
//
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpContext;
//
import java.io.IOException;
/**
Created by kurila on 30.01.15.
*/
public final class SharedHeadersAdder
implements HttpRequestInterceptor {
	//
	private final HeaderGroup sharedHeaders;
	//
	public
	SharedHeadersAdder(final HeaderGroup sharedHeaders) {
		this.sharedHeaders = sharedHeaders;
	}
	//
	@Override
	public final void process(
		final HttpRequest request, final HttpContext context
	) throws HttpException, IOException {
		for(final Header nextHeader : sharedHeaders.getAllHeaders()) {
			if(!request.containsHeader(nextHeader.getName())) {
				request.setHeader(nextHeader);
			}
		}
	}
}
