package com.emc.mongoose.common.net.ssl;
//
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
/**
 Created by andrey on 12.05.16.
 */
public final class X509TrustAllManager
implements X509TrustManager {
	//
	public final static X509TrustAllManager INSTANCE = new X509TrustAllManager();
	//
	@Override
	public final void checkClientTrusted(final X509Certificate[] x509Certificates, final String s)
	throws CertificateException {
	}
	//
	@Override
	public final void checkServerTrusted(final X509Certificate[] x509Certificates, final String s)
	throws CertificateException {
	}
	//
	@Override
	public final X509Certificate[] getAcceptedIssuers() {
		return null;
	}
}
