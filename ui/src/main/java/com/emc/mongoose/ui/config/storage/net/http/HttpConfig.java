package com.emc.mongoose.ui.config.storage.net.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 Created by andrey on 05.07.17.
 */
public final class HttpConfig
implements Serializable {

	public static final String KEY_FS_ACCESS = "fsAccess";
	public static final String KEY_HEADERS = "headers";
	public static final String KEY_HEADER_CONNECTION = "Connection";
	public static final String KEY_HEADER_USER_AGENT = "User-Agent";
	public static final String KEY_NAMESPACE = "namespace";
	public static final String KEY_VERSIONING = "versioning";

	public final void setFsAccess(final boolean fsAccess) {
		this.fsAccess = fsAccess;
	}

	public final void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public final void setVersioning(final boolean versioning) {
		this.versioning = versioning;
	}

	public final void setHeadersConfig(final Map<String, String> headers) {
		this.headersConfig = headers;
	}

	@JsonProperty(KEY_FS_ACCESS) private boolean fsAccess;
	@JsonProperty(KEY_NAMESPACE) private String namespace;
	@JsonProperty(KEY_VERSIONING) private boolean versioning;
	@JsonProperty(KEY_HEADERS) private Map<String, String> headersConfig;

	public HttpConfig() {
	}

	public HttpConfig(final HttpConfig other) {
		this.fsAccess = other.getFsAccess();
		this.namespace = other.getNamespace();
		this.versioning = other.getVersioning();
		this.headersConfig = new HashMap<>(other.getHeadersConfig());
	}

	public boolean getFsAccess() {
		return fsAccess;
	}

	public String getNamespace() {
		return namespace;
	}

	public boolean getVersioning() {
		return versioning;
	}

	public Map<String, String> getHeadersConfig() {
		return headersConfig;
	}
}