package com.emc.mongoose.core.api.item.data;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
//
/**
 Created by kurila on 29.09.14.
 Web storage data object.
 */
public interface HttpDataItem
extends MutableDataItem, HttpEntity {
	//
	Header HEADER_CONTENT_TYPE = new BasicHeader(
		HTTP.CONTENT_TYPE, BasicConfig.CONTEXT_CONFIG.get().getHttpContentType()
	);
	boolean
		IS_CONTENT_CHUNKED = BasicConfig.CONTEXT_CONFIG.get().getHttpContentChunked(),
		IS_CONTENT_REPEATABLE = BasicConfig.CONTEXT_CONFIG.get().getHttpContentRepeatable();
}
