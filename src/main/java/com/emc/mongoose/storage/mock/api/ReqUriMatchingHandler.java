package com.emc.mongoose.storage.mock.api;
//
import org.apache.http.HttpRequest;
//
import org.apache.http.HttpResponse;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
/**
 Created by andrey on 03.08.15.
 */
public interface ReqUriMatchingHandler<T extends HttpDataItemMock>
extends HttpAsyncRequestHandler<HttpRequest> {
	/**
	 Try to determine briefly if the specified URI matches this handler or not
	 @param httpRequest the HTTP request to analyze
	 @return true if request URI looks like acceptable by this handler else otherwise
	 */
	boolean matches(final HttpRequest httpRequest);
	/** Handle the request actually */
	void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String requestURI
	);
}
