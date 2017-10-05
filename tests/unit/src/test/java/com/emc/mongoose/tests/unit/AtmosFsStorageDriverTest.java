package com.emc.mongoose.tests.unit;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.data.BasicDataIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.BasicDataItem;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.http.emc.atmos.AtmosApi;
import com.emc.mongoose.storage.driver.net.http.emc.atmos.AtmosStorageDriver;
import com.emc.mongoose.storage.driver.net.http.emc.base.EmcConstants;
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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

import static com.emc.mongoose.storage.driver.net.http.emc.atmos.AtmosApi.SUBTENANT_URI_BASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AtmosFsStorageDriverTest
extends AtmosStorageDriver {

	private static final Credential CREDENTIAL = Credential.getInstance(
		"user1", "u5QtPuQx+W5nrrQQEg7nArBqSgC8qLiDt2RhQthb"
	);
	private static final String AUTH_TOKEN = "5cc597535ed747f09b5d273154216339";
	private static final String NS = "ns1";

	private static Config getConfig() {
		try {
			final Config config = Config.loadDefaults();
			final StorageConfig storageConfig = config.getStorageConfig();
			final HttpConfig httpConfig = storageConfig.getNetConfig().getHttpConfig();
			httpConfig.setNamespace(NS);
			httpConfig.setFsAccess(true);
			httpConfig.setVersioning(true);
			final AuthConfig authConfig = storageConfig.getAuthConfig();
			authConfig.setUid(CREDENTIAL.getUid());
			authConfig.setToken(AUTH_TOKEN);
			authConfig.setSecret(CREDENTIAL.getSecret());
			return config;
		} catch(final Throwable cause) {
			throw new RuntimeException(cause);
		}
	}

	private final Queue<FullHttpRequest> httpRequestsLog = new ArrayDeque<>();

	public AtmosFsStorageDriverTest()
	throws Exception {
		this(getConfig());
	}

	private AtmosFsStorageDriverTest(final Config config)
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
	public void testRequestNewAuthToken()
	throws Exception {

		requestNewAuthToken(credential);
		assertEquals(1, httpRequestsLog.size());

		final FullHttpRequest req = httpRequestsLog.poll();
		assertEquals(HttpMethod.PUT, req.method());
		assertEquals(AtmosApi.SUBTENANT_URI_BASE, req.uri());
		final HttpHeaders reqHeaders = req.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final Date reqDate = DateUtil.FMT_DATE_RFC1123.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), reqDate.getTime(), 10_000);
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertEquals(credential.getUid(), reqHeaders.get(EmcConstants.KEY_X_EMC_UID));
		final String sig = reqHeaders.get(EmcConstants.KEY_X_EMC_SIGNATURE);
		assertTrue(sig != null && sig.length() > 0);

		final String canonicalReq = getCanonical(reqHeaders, req.method(), req.uri());
		assertEquals(
			"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + SUBTENANT_URI_BASE + "\n"
				+ EmcConstants.KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED + ":true\n"
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ EmcConstants.KEY_X_EMC_UID + ':' + credential.getUid(),
			canonicalReq
		);
	}

	@Test
	public void testCreate()
	throws Exception {

		final long itemSize = 10240;
		final String dir = "/dir0";
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, dir, credential, null, 0
		);

		final HttpRequest req = getHttpRequest(ioTask, storageNodeAddrs[0]);
		assertEquals(HttpMethod.POST, req.method());
		assertEquals(AtmosApi.NS_URI_BASE + dir + '/' + itemId, req.uri());

		final HttpHeaders reqHeaders = req.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		assertEquals(itemSize, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final Date reqDate = DateUtil.FMT_DATE_RFC1123.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), reqDate.getTime(), 10_000);
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertEquals(
			AUTH_TOKEN + '/' + credential.getUid(), reqHeaders.get(EmcConstants.KEY_X_EMC_UID)
		);
		final String sig = reqHeaders.get(EmcConstants.KEY_X_EMC_SIGNATURE);
		assertTrue(sig != null && sig.length() > 0);

		final String canonicalReq = getCanonical(reqHeaders, req.method(), req.uri());
		assertEquals(
			"POST\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + req.uri() + "\n"
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ EmcConstants.KEY_X_EMC_UID + ':' + AUTH_TOKEN + '/' + credential.getUid(),
			canonicalReq
		);
	}
}
