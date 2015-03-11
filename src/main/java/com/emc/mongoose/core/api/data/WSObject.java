package com.emc.mongoose.core.api.data;
//
//
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.HTTP;
//
/**
 Created by kurila on 29.09.14.
 Web storage data object.
 */
public interface WSObject
extends DataObject, HttpEntity {
	//
	public final static Header HEADER_CONTENT_TYPE = new BasicHeader(
		HTTP.CONTENT_TYPE, RunTimeConfig.getContext().getHttpContentType()
	);
	public final static boolean
		IS_CONTENT_CHUNKED = RunTimeConfig.getContext().getHttpContentChunked(),
		IS_CONTENT_REPEATABLE = RunTimeConfig.getContext().getHttpContentRepeatable();
	//
	HttpEntity getPendingUpdatesContentEntity();
	//
	HttpEntity getPendingAugmentContentEntity();
	//
	boolean compareWith(final ContentDecoder in, final IOControl ioCtl);
}
