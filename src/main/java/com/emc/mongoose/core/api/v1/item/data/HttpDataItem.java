package com.emc.mongoose.core.api.v1.item.data;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
/**
 Created by kurila on 29.09.14.
 Web storage data object.
 */
public interface HttpDataItem
extends MutableDataItem, HttpEntity {
	//
	Header HEADER_CONTENT_TYPE = new BasicHeader(
		HTTP.CONTENT_TYPE, "application/octet-stream"
	);
	boolean
		IS_CONTENT_CHUNKED = false,
		IS_CONTENT_REPEATABLE = true;
}
