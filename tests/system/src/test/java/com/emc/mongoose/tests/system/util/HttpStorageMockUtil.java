package com.emc.mongoose.tests.system.util;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.NamingThreadFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 Created by kurila on 06.06.17.
 */
public interface HttpStorageMockUtil {

	ExecutorService REQ_EXECUTOR = Executors.newFixedThreadPool(
		ThreadUtil.getHardwareThreadCount(), new NamingThreadFactory("testHttpReqExecutor", true)
	);

	static void assertItemNotExists(final String nodeAddr, final String itemPath)
	throws MalformedURLException, IOException, InterruptedException, ExecutionException {
		final Future<Integer> futureRespCode = REQ_EXECUTOR.submit(
			() -> {
				final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
				final HttpURLConnection c = (HttpURLConnection) itemUrl.openConnection();
				c.setRequestMethod("GET");
				c.connect();
				try {
					return c.getResponseCode();
				} finally {
					c.disconnect();
				}
			}
		);
		assertEquals(404, futureRespCode.get().intValue());
	}

	static void assertItemExists(
		final String nodeAddr, final String itemPath, final long expectedSize
	) throws MalformedURLException, IOException, InterruptedException, ExecutionException {
		final Future<Long> futureContentLen = REQ_EXECUTOR.submit(
			() -> {
				final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
				return (long) itemUrl.openConnection().getContentLength();
			}
		);
		assertEquals(expectedSize, futureContentLen.get().longValue());
	}
}
