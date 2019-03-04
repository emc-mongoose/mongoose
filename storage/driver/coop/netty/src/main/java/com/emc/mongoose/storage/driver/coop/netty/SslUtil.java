package com.emc.mongoose.storage.driver.coop.netty;

import io.netty.handler.ssl.OpenSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public interface SslUtil {

	SslContext CLIENT_SSL_CONTEXT = sslContext();

	static SslContext sslContext() {
		try {
			return SslContextBuilder
							.forClient()
							.trustManager(InsecureTrustManagerFactory.INSTANCE)
							.sslProvider(OpenSslContext.defaultClientProvider())
							.protocols("TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3")
							.ciphers(Arrays.asList(SSLContext.getDefault().getServerSocketFactory().getSupportedCipherSuites()))
							.build();
		} catch (final NoSuchAlgorithmException | SSLException e) {
			throw new AssertionError(e);
		}
	}
}
