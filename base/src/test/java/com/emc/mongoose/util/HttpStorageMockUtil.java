package com.emc.mongoose.util;

import static org.junit.Assert.assertEquals;

import com.emc.mongoose.base.logging.LogContextThreadFactory;
import com.github.akurilov.commons.concurrent.ThreadUtil;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/** Created by kurila on 06.06.17. */
public interface HttpStorageMockUtil {

	ExecutorService REQ_EXECUTOR = Executors.newFixedThreadPool(
					ThreadUtil.getHardwareThreadCount(),
					new LogContextThreadFactory("testHttpReqExecutor", true));

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
						});
		try {
			assertEquals(404, futureRespCode.get().intValue());
		} catch (final InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	static int getContentLength(final String nodeAddr, final String itemPath) {
		final Future<Integer> futureContentLen = REQ_EXECUTOR.submit(
						() -> {
							final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
							return itemUrl.openConnection().getContentLength();
						});
		try {
			return futureContentLen.get();
		} catch (final InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	static void assertItemExists(
					final String nodeAddr, final String itemPath, final long expectedSize) {
		final int actualSize = getContentLength(nodeAddr, itemPath);
		assertEquals("Invalid size returned for the \"" + itemPath + "\"", expectedSize, actualSize);
	}

	static void checkItemContent(
					final String nodeAddr, final String itemPath, final Consumer<byte[]> checkContentFunc) {
		final Future<byte[]> futureContentLen = REQ_EXECUTOR.submit(
						() -> {
							final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
							byte[] buff = null;
							HttpURLConnection conn = null;
							try {
								conn = (HttpURLConnection) itemUrl.openConnection();
								final int contentLen = conn.getContentLength();
								buff = new byte[contentLen];
								try (final InputStream in = conn.getInputStream()) {
									int offset = 0, n;
									while (offset < contentLen) {
										n = in.read(buff, offset, contentLen - offset);
										if (n < 0) {
											break;
										} else {
											offset += n;
										}
									}
								}
							} finally {
								if (conn != null) {
									conn.disconnect();
								}
							}
							return buff;
						});
		try {
			checkContentFunc.accept(futureContentLen.get());
		} catch (final InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
