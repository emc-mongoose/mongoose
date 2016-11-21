package com.emc.mongoose.storage.driver.net.http.swift;
/**
 Created by andrey on 07.10.16.
 */
public interface SwiftConstants {

	String KEY_X_AUTH_KEY = "X-Auth-Key";
	String KEY_X_AUTH_TOKEN = "X-Auth-Token";
	String KEY_X_AUTH_USER = "X-Auth-User";
	String KEY_X_COPY_FROM = "X-Copy-From";
	String KEY_X_VERSIONS_LOCATION = "X-Versions-Location";

	String URI_BASE = "/v1";
	String AUTH_URI = "/auth/v1.0";
	String DEFAULT_VERSIONS_LOCATION = "archive";
}
