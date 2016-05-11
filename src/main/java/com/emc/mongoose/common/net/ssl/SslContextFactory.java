package com.emc.mongoose.common.net.ssl;
//
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
/**
 Created by andrey on 11.05.16.
 */
public abstract class SslContextFactory {
	//
	private SslContextFactory() {}
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static char[] KEY_STORE_PASSWD = "mongoose".toCharArray();
	//
	public static SSLContext getInstance() {
		//
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("TLS");
		} catch(final NoSuchAlgorithmException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to get the SSL context instance");
		}
		final ClassLoader clsLoader = SslContextFactory.class.getClassLoader();
		// keystore
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch(final KeyStoreException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to get the keystore instance");
		}
		final URL urlKeyStore = clsLoader.getResource("default.jks");
		if(keyStore != null) {
			try {
				if(urlKeyStore == null) {
					keyStore.load(null);
				} else {
					try(final InputStream is = urlKeyStore.openStream()) {
						keyStore.load(is, KEY_STORE_PASSWD);
					}
				}
			} catch(final IOException | NoSuchAlgorithmException | CertificateException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to load the keystore");
			}
			KeyManagerFactory kmf = null;
			try {
				kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keyStore, KEY_STORE_PASSWD);
			} catch(final NoSuchAlgorithmException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to get the key manager factory");
			} catch(final KeyStoreException | UnrecoverableKeyException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to init the key manager factory");
			}
			//
			TrustManagerFactory tmf = null;
			try {
				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(keyStore);
			} catch(final NoSuchAlgorithmException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to get the trust manager factory");
			} catch(final KeyStoreException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to init the trust manager factory");
			}

			// certificate
			final URL urlCert = clsLoader.getResource("root.cer");
			CertificateFactory cf = null;
			try {
				cf = CertificateFactory.getInstance("X.509");
			} catch(final CertificateException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to get the certificate factory");
			}
			if(urlCert != null && cf != null) {
				try(final InputStream is = urlCert.openStream()) {
					final X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
					keyStore.setCertificateEntry("selfsigned", cert);
				} catch(final IOException | CertificateException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to load the certificate");
				} catch(final KeyStoreException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to set the keystore cert entry");
				}
			}
			//
			if(sslContext != null) {
				try {
					sslContext.init(
						kmf == null ? null : kmf.getKeyManagers(),
						tmf == null ? null : tmf.getTrustManagers(),
						new SecureRandom()
					);
				} catch(final KeyManagementException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to init the SSL context");
				}
			}
		}
		//
		return sslContext;
	}
}
