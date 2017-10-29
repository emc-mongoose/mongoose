package com.emc.mongoose.ui.config.storage.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class AuthConfig
implements Serializable {

	public static final String KEY_FILE = "file";
	public static final String KEY_SECRET = "secret";
	public static final String KEY_TOKEN = "token";
	public static final String KEY_UID = "uid";

	public final void setFile(final String file) {
		this.file = file;
	}

	public final void setSecret(final String secret) {
		this.secret = secret;
	}

	public final void setToken(final String token) {
		this.token = token;
	}

	public final void setUid(final String uid) {
		this.uid = uid;
	}

	@JsonProperty(KEY_FILE) private String file;
	@JsonProperty(KEY_SECRET) private String secret;
	@JsonProperty(KEY_TOKEN) private String token;
	@JsonProperty(KEY_UID) private String uid;

	public AuthConfig() {
	}

	public AuthConfig(final AuthConfig other) {
		this.file = other.getFile();
		this.secret = other.getSecret();
		this.token = other.getToken();
		this.uid = other.getUid();
	}

	public final String getFile() {
		return file;
	}

	public final String getSecret() {
		return secret;
	}

	public final String getToken() {
		return token;
	}

	public final String getUid() {
		return uid;
	}
}