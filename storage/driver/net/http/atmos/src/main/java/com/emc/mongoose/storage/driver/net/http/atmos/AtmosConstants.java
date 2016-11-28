package com.emc.mongoose.storage.driver.net.http.atmos;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;

/**
 Created by kurila on 11.11.16.
 */
public interface AtmosConstants {

	String URI_BASE = "/rest";

	String OBJ_URI_BASE = URI_BASE + "/objects";

	String NS_URI_BASE = URI_BASE + "/namespace";

	String SUBTENANT_URI_BASE = URI_BASE + "/subtenant";

	String SIGN_METHOD = "HmacSHA1";

	String KEY_SUBTENANT_ID = "subtenantID";

	AsciiString HEADERS_CANONICAL[] = {
		//HttpHeaderNames.CONTENT_MD5,
		HttpHeaderNames.CONTENT_TYPE,
		HttpHeaderNames.RANGE,
		HttpHeaderNames.DATE
	};

}
