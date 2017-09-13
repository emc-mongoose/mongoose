package com.emc.mongoose.api.common.net.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 Created by kurila on 12.05.16.
 */
public final class X509TrustAllManager
implements X509TrustManager {

	private X509TrustAllManager() {}

	public static final X509TrustAllManager INSTANCE = new X509TrustAllManager();

	@Override
	public final void checkClientTrusted(final X509Certificate[] x509Certificates, final String s)
	throws CertificateException {
	}

	@Override
	public final void checkServerTrusted(final X509Certificate[] x509Certificates, final String s)
	throws CertificateException {
	}

	@Override
	public final X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}
}
