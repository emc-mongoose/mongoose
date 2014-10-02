package com.emc.mongoose.object.data;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
/**
 Created by kurila on 29.09.14.
 Web storage data object.
 */
public interface WSDataObject
extends DataObject, HttpEntity {
	//
	Header
		HEADER_CONTENT_TYPE = new BasicHeader(
			HttpHeaders.CONTENT_TYPE, RunTimeConfig.getString("http.content.type")
		);
	boolean
		IS_CONTENT_REPEATABLE = RunTimeConfig.getBoolean("http.content.repeatable"),
		IS_CONTENT_CHUNKED = RunTimeConfig.getBoolean("http.content.chunked");
	//
	HttpEntity getPendingUpdatesContentEntity();
	//
	HttpEntity getPendingAugmentContentEntity();
	//
}
