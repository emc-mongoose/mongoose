package com.emc.mongoose.model.storage;

/**
 Created by andrey on 14.03.17.
 */
public final class BasicCredential
implements Credential {

	private final String uid;
	private final String secret;

	protected BasicCredential(final String uid, final String secret) {
		this.uid = uid;
		this.secret = secret;
	}

	@Override
	public final String getUid() {
		return uid;
	}

	@Override
	public final String getSecret() {
		return secret;
	}
}
