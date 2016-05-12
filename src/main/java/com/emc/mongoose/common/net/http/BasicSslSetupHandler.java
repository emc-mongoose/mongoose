package com.emc.mongoose.common.net.http;
//
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
//
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.security.NoSuchAlgorithmException;

/**
 Created by andrey on 11.05.16.
 */
public final class BasicSslSetupHandler
implements SSLSetupHandler {
	//
	private BasicSslSetupHandler() {}
	//
	public final static BasicSslSetupHandler INSTANCE = new BasicSslSetupHandler();
	//
	@Override
	public final void initalize(final SSLEngine sslEngine)
	throws SSLException {
		sslEngine.setEnabledProtocols(
			new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3" }
		);
		final SSLContext sslContext;
		try {
			sslContext = SSLContext.getDefault();
		} catch(final NoSuchAlgorithmException e) {
			throw new SSLException(e);
		}
		sslEngine.setEnabledCipherSuites(
			sslContext.getServerSocketFactory().getSupportedCipherSuites()
		);
	}
	//
	@Override
	public final void verify(final IOSession ioSession, final SSLSession sslSession)
	throws SSLException {
	}
}
