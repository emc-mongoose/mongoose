package com.emc.mongoose.storage.driver.net.http.atmos;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;
/**
 Created by kurila on 11.11.16.
 */
public interface AtmosConstants {
	String OBJ_URI_BASE = "/rest/objects";
	String NS_URI_BASE = "/rest/namespace";
	String SIGN_METHOD = "HmacSHA1";
	AsciiString HEADERS_CANONICAL[] = {
		HttpHeaderNames.CONTENT_MD5, HttpHeaderNames.CONTENT_TYPE, HttpHeaderNames.RANGE,
		HttpHeaderNames.DATE
	};

}
