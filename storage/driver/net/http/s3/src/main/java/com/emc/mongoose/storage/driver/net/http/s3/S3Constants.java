package com.emc.mongoose.storage.driver.net.http.s3;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;

import java.nio.charset.StandardCharsets;

/**
 Created by kurila on 02.08.16.
 */
public interface S3Constants {

	String PREFIX_KEY_X_AMZ = "x-amz-";

	String AUTH_PREFIX = "AWS ";

	String KEY_X_AMZ_COPY_SOURCE = PREFIX_KEY_X_AMZ + "copy-source";

	String KEY_X_AMZ_SECURITY_TOKEN = PREFIX_KEY_X_AMZ + "security-token";

	AsciiString HEADERS_CANONICAL[] = {
		HttpHeaderNames.CONTENT_MD5, HttpHeaderNames.CONTENT_TYPE, HttpHeaderNames.RANGE,
		HttpHeaderNames.DATE
	};

	String URL_ARG_VERSIONING = "versioning";

	String SIGN_METHOD = "HmacSHA1";

	byte[] VERSIONING_ENABLE_CONTENT = (
		"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
			"<Status>Enabled</Status></VersioningConfiguration>"
		).getBytes(StandardCharsets.US_ASCII);

	byte[] VERSIONING_DISABLE_CONTENT = (
		"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
			"<Status>Suspended</Status></VersioningConfiguration>"
		).getBytes(StandardCharsets.US_ASCII);
}
