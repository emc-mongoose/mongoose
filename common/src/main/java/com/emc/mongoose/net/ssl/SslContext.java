package com.emc.mongoose.net.ssl;

import com.emc.mongoose.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	private final static Logger LOG = LogManager.getLogger();

	private static SSLContext getInstance() {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("TLS");
		} catch(final NoSuchAlgorithmException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to get the SSL context instance"
			);
		}
		if(sslContext != null) {
			try {
				sslContext.init(
					null, new TrustManager[] { X509TrustAllManager.INSTANCE },
					new SecureRandom()
				);
			} catch(final KeyManagementException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to init the SSL context"
				);
			}
		}
		//
		return sslContext;
	}

	public static volatile SSLContext INSTANCE = getInstance();
}
