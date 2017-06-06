package com.emc.mongoose.tests.system.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 Created by kurila on 06.06.17.
 */
public interface HttpStorageMockUtil {
	
	static void checkItem(final String nodeAddr, final String itemPath, final long expectedSize)
	throws MalformedURLException, IOException {
		final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
		long size = 0;
		int n;
		final byte buff[] = new byte[0x1000];
		try(final InputStream in = itemUrl.openStream()) {
			while(- 1 != (n = in.read(buff))) {
				size += n;
			}
		}
		assertEquals(expectedSize, size);
	}
}
