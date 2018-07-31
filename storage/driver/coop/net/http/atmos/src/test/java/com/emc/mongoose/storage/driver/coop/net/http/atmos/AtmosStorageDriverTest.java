package com.emc.mongoose.storage.driver.coop.net.http.atmos;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.DateUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.item.DataItemImpl;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.data.DataOperationImpl;
import com.emc.mongoose.item.op.data.DataOperation;
import com.emc.mongoose.storage.Credential;
import com.emc.mongoose.storage.driver.coop.net.http.EmcConstants;
import com.github.akurilov.commons.collection.TreeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.impl.BasicConfig;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.SUBTENANT_URI_BASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AtmosStorageDriverTest
extends AtmosStorageDriver {

	private static final Credential CREDENTIAL = Credential.getInstance(
		"user1", "u5QtPuQx+W5nrrQQEg7nArBqSgC8qLiDt2RhQthb"
	);
	private static final String AUTH_TOKEN = "5cc597535ed747f09b5d273154216339";
	private static final String NS = "ns1";

	private static Config getConfig() {
		try {
			final List<Map<String, Object>> configSchemas = Extension
				.load(Thread.currentThread().getContextClassLoader())
				.stream()
				.map(Extension::schemaProvider)
				.filter(Objects::nonNull)
				.map(
					schemaProvider -> {
						try {
							return schemaProvider.schema();
						} catch(final Exception e) {
							fail(e.getMessage());
						}
						return null;
					}
				)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
			SchemaProvider
				.resolve(APP_NAME, Thread.currentThread().getContextClassLoader())
				.stream()
				.findFirst()
				.ifPresent(configSchemas::add);
			final Map<String, Object> configSchema = TreeUtil.reduceForest(configSchemas);
			final Config config = new BasicConfig("-", configSchema);
			config.val("load-batch-size", 4096);
			config.val("storage-driver-limit-concurrency", 0);
			config.val("storage-net-transport", "epoll");
			config.val("storage-net-reuseAddr", true);
			config.val("storage-net-bindBacklogSize", 0);
			config.val("storage-net-keepAlive", true);
			config.val("storage-net-rcvBuf", 0);
			config.val("storage-net-sndBuf", 0);
			config.val("storage-net-ssl", false);
			config.val("storage-net-tcpNoDelay", false);
			config.val("storage-net-interestOpQueued", false);
			config.val("storage-net-linger", 0);
			config.val("storage-net-timeoutMilliSec", 0);
			config.val("storage-net-ioRatio", 50);
			config.val("storage-net-node-addrs", Collections.singletonList("127.0.0.1"));
			config.val("storage-net-node-port", 9024);
			config.val("storage-net-node-connAttemptsLimit", 0);
			config.val("storage-net-http-namespace", NS);
			config.val("storage-net-http-fsAccess", false);
			config.val("storage-net-http-versioning", true);
			config.val("storage-net-http-headers", Collections.EMPTY_MAP);
			config.val("storage-auth-uid", CREDENTIAL.getUid());
			config.val("storage-auth-token", AUTH_TOKEN);
			config.val("storage-auth-secret", CREDENTIAL.getSecret());
			config.val("storage-driver-threads", 0);
			config.val("storage-driver-limit-queue-input", 1_000_000);
			config.val("storage-driver-limit-queue-output", 1_000_000);
			return config;
		} catch(final Throwable cause) {
			throw new RuntimeException(cause);
		}
	}

	private final Queue<FullHttpRequest> httpRequestsLog = new ArrayDeque<>();

	public AtmosStorageDriverTest()
	throws Exception {
		this(getConfig());
	}

	private AtmosStorageDriverTest(final Config config)
	throws Exception {
		super(
			"test-storage-driver-atmos",
			DataInput.instance(null, "7a42d9c483244167", new SizeInBytes("4MB"), 16),
			config.configVal("storage"), false, config.intVal("load-batch-size")
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
		assertEquals(SUBTENANT_URI_BASE, req.uri());
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
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ EmcConstants.KEY_X_EMC_UID + ':' + credential.getUid(),
			canonicalReq
		);
	}

	@Test
	public void testRead()
	throws Exception {

		final long itemSize = 10240;
		final String itemId = "4fccd760a1f2194004fcce05b010a304ffc5aa15c541";
		final DataItem dataItem = new DataItemImpl(
			itemId, Long.parseLong("00003brre8lgz", Character.MAX_RADIX), itemSize
		);
		final DataOperation<DataItem> op = new DataOperationImpl<>(
			hashCode(), OpType.READ, dataItem, null, null, credential, null, 0
		);

		final HttpRequest req = httpRequest(op, storageNodeAddrs[0]);
		assertEquals(HttpMethod.GET, req.method());
		assertEquals(AtmosApi.OBJ_URI_BASE + '/' + itemId, req.uri());

		final HttpHeaders reqHeaders = req.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
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
			"GET\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + req.uri() + "\n"
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ EmcConstants.KEY_X_EMC_UID + ':' + AUTH_TOKEN + '/' + credential.getUid(),
			canonicalReq
		);
	}
}
