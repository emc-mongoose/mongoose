package com.emc.mongoose.tests.system.util;

import com.emc.mongoose.api.common.concurrent.ThreadUtil;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;

import java.net.HttpURLConnection;
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
		ThreadUtil.getHardwareThreadCount(), new LogContextThreadFactory("testHttpReqExecutor", true)
	);

	static void assertItemNotExists(final String nodeAddr, final String itemPath) {
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
		try {
			assertEquals(404, futureRespCode.get().intValue());
		} catch(final InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	static void assertItemExists(
		final String nodeAddr, final String itemPath, final long expectedSize
	) {
		final Future<Long> futureContentLen = REQ_EXECUTOR.submit(
			() -> {
				final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
				return (long) itemUrl.openConnection().getContentLength();
			}
		);
		try {
			final long actualSize = futureContentLen.get();
			assertEquals(
				"Invalid size returned for the \"" + itemPath + "\"", expectedSize, actualSize
			);
		} catch(final InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
