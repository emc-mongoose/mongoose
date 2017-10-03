package com.emc.mongoose.tests.unit;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.item.BasicDataItem;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api;
import com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3StorageDriver;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.storage.net.http.HttpConfig;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmzS3StorageDriverTest
extends AmzS3StorageDriver {

	private static final String UID = "user1";
	private static final String SECRET = "u5QtPuQx+W5nrrQQEg7nArBqSgC8qLiDt2RhQthb";

	private static Config getConfig() {
		try {
			final Config config = Config.loadDefaults();
			final StorageConfig storageConfig = config.getStorageConfig();
			final HttpConfig httpConfig = storageConfig.getNetConfig().getHttpConfig();
			httpConfig.setNamespace("ns1");
			httpConfig.setFsAccess(true);
			httpConfig.setVersioning(true);
			final AuthConfig authConfig = storageConfig.getAuthConfig();
			authConfig.setUid(UID);
			authConfig.setSecret(SECRET);
			return config;
		} catch(final Throwable cause) {
			throw new RuntimeException(cause);
		}
	}

	private final Queue<FullHttpRequest> httpRequestsLog = new ArrayDeque<>();

	public AmzS3StorageDriverTest()
	throws Exception {
		this(getConfig());
	}

	private AmzS3StorageDriverTest(final Config config)
	throws Exception {
		super(
			config.getTestConfig().getStepConfig().getId(),
			DataInput.getInstance(
				config.getItemConfig().getDataConfig().getInputConfig().getFile(),
				config.getItemConfig().getDataConfig().getInputConfig().getSeed(),
				config.getItemConfig().getDataConfig().getInputConfig().getLayerConfig().getSize(),
				config.getItemConfig().getDataConfig().getInputConfig().getLayerConfig().getCache()
			),
			config.getLoadConfig(), config.getStorageConfig(),
			config.getItemConfig().getDataConfig().getVerify()
		);
	}

	@Override
	protected FullHttpResponse executeHttpRequest(final FullHttpRequest httpRequest) {
		httpRequestsLog.add(httpRequest);
		return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
	}

	@After
	public void tearDown() {
		httpRequestsLog.clear();
	}

	@Test
	public void testRequestNewPath()
	throws Exception {

		final String bucketName = "/bucket0";
		final String result = requestNewPath(bucketName);
		assertEquals(bucketName, result);
		assertEquals(3, httpRequestsLog.size());

		final FullHttpRequest req0 = httpRequestsLog.poll();
		assertEquals(HttpMethod.HEAD, req0.method());
		assertEquals(bucketName, req0.uri());
		final HttpHeaders reqHeaders0 = req0.headers();
		assertEquals("127.0.0.1:9020", reqHeaders0.get(HttpHeaderNames.HOST));
		assertEquals(0, reqHeaders0.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final Date reqDate0 = DateUtil.FMT_DATE_RFC1123.parse(
			reqHeaders0.get(HttpHeaderNames.DATE)
		);
		assertEquals(new Date().getTime(), reqDate0.getTime(), 1_000_000);
		final String authHeaderValue0 = reqHeaders0.get(HttpHeaderNames.AUTHORIZATION);
		assertTrue(authHeaderValue0.startsWith("AWS " + UID + ":"));

		final FullHttpRequest req1 = httpRequestsLog.poll();
		assertEquals(HttpMethod.GET, req1.method());
		assertEquals(bucketName + "?versioning", req1.uri());
		final HttpHeaders reqHeaders1 = req1.headers();
		assertEquals("127.0.0.1:9020", reqHeaders1.get(HttpHeaderNames.HOST));
		assertEquals(0, reqHeaders1.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final Date reqDate1 = DateUtil.FMT_DATE_RFC1123.parse(
			reqHeaders1.get(HttpHeaderNames.DATE)
		);
		assertEquals(new Date().getTime(), reqDate1.getTime(), 1_000_000);
		final String authHeaderValue1 = reqHeaders1.get(HttpHeaderNames.AUTHORIZATION);
		assertTrue(authHeaderValue1.startsWith("AWS " + UID + ":"));

		final FullHttpRequest req2 = httpRequestsLog.poll();
		assertEquals(HttpMethod.PUT, req2.method());
		assertEquals(bucketName + "?versioning", req2.uri());
		final HttpHeaders reqHeaders2 = req2.headers();
		assertEquals("127.0.0.1:9020", reqHeaders2.get(HttpHeaderNames.HOST));
		final Date reqDate2 = DateUtil.FMT_DATE_RFC1123.parse(
			reqHeaders2.get(HttpHeaderNames.DATE)
		);
		assertEquals(new Date().getTime(), reqDate2.getTime(), 1_000_000);
		final String authHeaderValue2 = reqHeaders2.get(HttpHeaderNames.AUTHORIZATION);
		assertTrue(authHeaderValue2.startsWith("AWS " + UID + ":"));
		final byte[] reqContent2 = req2.content().array();
		assertEquals(AmzS3Api.VERSIONING_ENABLE_CONTENT, reqContent2);
		assertEquals(
			reqContent2.length, reqHeaders2.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue()
		);
	}

	@Test
	public void testBucketListing()
	throws Exception {

		final ItemFactory itemFactory = ItemType.getItemFactory(ItemType.DATA);

		final List<DataItem> dataItems = list(
			itemFactory, "/bucket1", "prefix", itemFactory.getItem()

		);
	}
}
