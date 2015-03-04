package com.emc.mongoose.object.api;
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
	WSIOTask.HTTPMethod getMethod();
	MutableWSRequest setMethod(final WSIOTask.HTTPMethod method);
	//
	String getUriAddr();
	MutableWSRequest setUriAddr(final String addr);
	//
	String getUriPath();
	MutableWSRequest setUriPath(final String path);
	//
	void clearHeaders();
}
