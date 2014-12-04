package com.emc.mongoose.web.api;
//
import org.apache.http.HttpRequest;
//
import java.net.URI;
import java.net.URISyntaxException;
/**
 Created by kurila on 04.12.14.
 */
public interface MutableHTTPRequest
extends HttpRequest {
	//
	WSIOTask.HTTPMethod getMethod();
	void setMethod(final WSIOTask.HTTPMethod method);
	//
	URI getURI();
	void setUri(final String uri);
}
