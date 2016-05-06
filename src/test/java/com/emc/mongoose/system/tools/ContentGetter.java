package com.emc.mongoose.system.tools;

import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

/**
 * Created by olga on 02.07.15.
 */
public final class ContentGetter {

	public static InputStream getStream(final String dataID, final String bucketName)
	throws IOException, NoSuchAlgorithmException {
		// There is url string w/o data ID
		final String firstPartURLString = String.format("http://127.0.0.1:9020/%s/", bucketName);
		final URL url = new URL(firstPartURLString+dataID);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.addRequestProperty(HttpHeaders.AUTHORIZATION, "AWS wuser1@sanity.local:vegpRvQdGFKmIvwIH6qErb5ekd8=");
		return connection.getInputStream();
	}

	public static int getDataSize(final String dataID, final String bucketName)
	throws IOException, NoSuchAlgorithmException {
		final byte[] buffer = new byte[1024];
		int numRead;
		int countByte = 0;

		try (final InputStream inputStream = getStream(dataID, bucketName)) {
			do {
				numRead = inputStream.read(buffer);
				if (numRead > 0) {
					countByte += numRead;
				}
			} while (numRead != -1);
		}
		return countByte;
	}
}
