package com.emc.mongoose.base.storage;

/** Created by andrey on 14.03.17. */
public interface Credential {

	String getUid();

	String getSecret();

	static Credential getInstance(final String uid, final String secret) {
		if (uid == null && secret == null) {
			return null;
		}
		return new CredentialImpl(uid, secret);
	}

	Credential NONE = new CredentialImpl(null, null);
}
