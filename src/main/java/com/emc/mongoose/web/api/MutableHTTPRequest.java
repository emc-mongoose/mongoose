package com.emc.mongoose.web.api;
//
import org.apache.http.HttpEntityEnclosingRequest;
/**
 Created by kurila on 04.12.14.
 */
public interface MutableHTTPRequest
extends HttpEntityEnclosingRequest {
	//
	WSIOTask.HTTPMethod getMethod();
	MutableHTTPRequest setMethod(final WSIOTask.HTTPMethod method);
	//
	String getUriAddr();
	MutableHTTPRequest setUriAddr(final String addr);
	//
	String getUriPath();
	MutableHTTPRequest setUriPath(final String path);
}
