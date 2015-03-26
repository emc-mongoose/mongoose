package com.emc.mongoose.core.api.io.req;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.entity.ByteArrayEntity;
/**
 Created by kurila on 04.12.14.
 */
public interface MutableWSRequest
extends HttpEntityEnclosingRequest {
	//
	HttpEntity EMPTY_CONTENT_ENTITY = new ByteArrayEntity(new byte[]{});
	//
	HTTPMethod getMethod();
	MutableWSRequest setMethod(final HTTPMethod method);
	//
	String getUriAddr();
	MutableWSRequest setUriAddr(final String addr);
	//
	String getUriPath();
	MutableWSRequest setUriPath(final String path);
	//
	void clearHeaders();
	//
	enum HTTPMethod { DELETE, GET, HEAD, PUT, POST, TRACE }
}
