package com.emc.mongoose.object.data;
//
import com.emc.mongoose.run.Main;
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
public interface WSObject
extends DataObject, HttpEntity {
	//
	Header
		HEADER_CONTENT_TYPE = new BasicHeader(
			HttpHeaders.CONTENT_TYPE, Main.RUN_TIME_CONFIG.getHttpContentType()
		);
	boolean
		IS_CONTENT_REPEATABLE = Main.RUN_TIME_CONFIG.getHttpContentRepeatable(),
		IS_CONTENT_CHUNKED = Main.RUN_TIME_CONFIG.getHttpContentChunked();
	//
	HttpEntity getPendingUpdatesContentEntity();
	//
	HttpEntity getPendingAugmentContentEntity();
	//
}
