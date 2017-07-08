package com.emc.mongoose.common.net.ssl;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.Fireball;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 Created by kurila on 12.05.16.
 */
public final class SslContext {

	private SslContext() {}

	private static SSLContext getInstance()
	throws OmgDoesNotPerformException {
		try {
			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(
				null, new TrustManager[] { X509TrustAllManager.INSTANCE }, new SecureRandom()
			);
			return sslContext;
		} catch(final NoSuchAlgorithmException | KeyManagementException e) {
			throw new OmgDoesNotPerformException(e);
		}
	}

	public static volatile SSLContext INSTANCE;
	static {
		try {
			INSTANCE = getInstance();
		} catch(final Fireball e) {
			e.printStackTrace(System.err);
		}
	}
}
