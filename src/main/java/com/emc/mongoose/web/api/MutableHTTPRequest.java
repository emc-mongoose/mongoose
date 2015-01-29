package com.emc.mongoose.web.api;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.entity.ByteArrayEntity;
/**
 Created by kurila on 04.12.14.
 */
public interface MutableHTTPRequest
extends HttpEntityEnclosingRequest {
	//
	HttpEntity EMPTY_CONTENT_ENTITY = new ByteArrayEntity(new byte[]{});
	//
	WSIOTask.HTTPMethod getMethod();
	MutableHTTPRequest setMethod(final WSIOTask.HTTPMethod method);
	//
	String getUriAddr();
	MutableHTTPRequest setUriAddr(final String addr);
	//
	String getUriPath();
	MutableHTTPRequest setUriPath(final String path);
	//
	void clearHeaders();
}
