package com.emc.mongoose.tests.unit;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.composite.data.BasicCompositeDataIoTask;
import com.emc.mongoose.api.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.api.model.io.task.data.BasicDataIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.api.model.item.BasicDataItem;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api;
import com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3StorageDriver;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.storage.net.http.HttpConfig;

import com.github.akurilov.commons.collection.Range;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import org.junit.After;
import org.junit.Test;

import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.KEY_UPLOAD_ID;
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
		assertEquals(storageNodeAddrs[0], reqHeaders0.get(HttpHeaderNames.HOST));
		assertEquals(0, reqHeaders0.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final Date reqDate0 = DateUtil.FMT_DATE_RFC1123.parse(
			reqHeaders0.get(HttpHeaderNames.DATE)
		);
		assertEquals(new Date().getTime(), reqDate0.getTime(), 10_000);
		final String authHeaderValue0 = reqHeaders0.get(HttpHeaderNames.AUTHORIZATION);
		assertTrue(authHeaderValue0.startsWith("AWS " + UID + ":"));

		final FullHttpRequest req1 = httpRequestsLog.poll();
		assertEquals(HttpMethod.GET, req1.method());
		assertEquals(bucketName + "?versioning", req1.uri());
		final HttpHeaders reqHeaders1 = req1.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders1.get(HttpHeaderNames.HOST));
		assertEquals(0, reqHeaders1.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final Date reqDate1 = DateUtil.FMT_DATE_RFC1123.parse(
			reqHeaders1.get(HttpHeaderNames.DATE)
		);
		assertEquals(
			"Date differs from now " + new Date() + " more than 10 sec: " + reqDate1,
			new Date().getTime(), reqDate1.getTime(), 10_000
		);
		final String authHeaderValue1 = reqHeaders1.get(HttpHeaderNames.AUTHORIZATION);
		assertTrue(authHeaderValue1.startsWith("AWS " + UID + ":"));

		final FullHttpRequest req2 = httpRequestsLog.poll();
		assertEquals(HttpMethod.PUT, req2.method());
		assertEquals(bucketName + "?versioning", req2.uri());
		final HttpHeaders reqHeaders2 = req2.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders2.get(HttpHeaderNames.HOST));
		final Date reqDate2 = DateUtil.FMT_DATE_RFC1123.parse(
			reqHeaders2.get(HttpHeaderNames.DATE)
		);
		assertEquals(new Date().getTime(), reqDate2.getTime(), 10_000);
		final String authHeaderValue2 = reqHeaders2.get(HttpHeaderNames.AUTHORIZATION);
		assertTrue(authHeaderValue2.startsWith("AWS " + UID + ":"));
		final byte[] reqContent2 = req2.content().array();
		assertEquals(AmzS3Api.VERSIONING_ENABLE_CONTENT, reqContent2);
		assertEquals(
			reqContent2.length, reqHeaders2.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue()
		);
	}

	@Test @SuppressWarnings("unchecked")
	public void testBucketListing()
	throws Exception {

		final ItemFactory itemFactory = ItemType.getItemFactory(ItemType.DATA);
		final String bucketName = "/bucket1";
		final String itemPrefix = "0000";
		final String markerItemId = "00003brre8lgz";
		final Item markerItem = itemFactory.getItem(
			markerItemId, Long.parseLong(markerItemId, Character.MAX_RADIX), 10240
		);

		final List<Item> items = list(
			itemFactory, bucketName, itemPrefix, Character.MAX_RADIX, markerItem, 1000
		);

		assertEquals(0, items.size());
		assertEquals(1, httpRequestsLog.size());
		final FullHttpRequest httpRequest = httpRequestsLog.poll();
		assertEquals(HttpMethod.GET, httpRequest.method());
		final String reqUri = httpRequest.uri();
		assertEquals(
			bucketName + "?prefix=" + itemPrefix + "&marker=" + markerItemId + "&max-keys=1000",
			reqUri
		);
		final HttpHeaders httpHeaders = httpRequest.headers();
		assertEquals(0, httpHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertEquals(storageNodeAddrs[0], httpHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(httpHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertTrue(httpHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));
	}

	@Test
	public void testCreateRequest()
	throws Exception {

		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.PUT, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(itemSize, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testCopyRequest()
	throws Exception {

		final String bucketSrcName = "/bucketSrc";
		final String bucketDstName = "/bucketDst";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, bucketSrcName, bucketDstName,
			Credential.getInstance(UID, SECRET), null, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.PUT, httpRequest.method());
		assertEquals(bucketDstName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(bucketSrcName + "/" + itemId, reqHeaders.get(AmzS3Api.KEY_X_AMZ_COPY_SOURCE));
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + '\n'
				+ AmzS3Api.KEY_X_AMZ_COPY_SOURCE + ':' + bucketSrcName + '/' + itemId + '\n'
				+ bucketDstName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testMpuInitRequest()
	throws Exception {

		final String bucketName = "/bucket3";
		final long itemSize = 12345;
		final long partSize = 1234;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final CompositeDataIoTask<DataItem> mpuTask = new BasicCompositeDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0, partSize
		);

		final HttpRequest httpRequest = getHttpRequest(mpuTask, storageNodeAddrs[0]);
		final HttpHeaders reqHeaders = httpRequest.headers();

		assertEquals(HttpMethod.POST, httpRequest.method());
		assertEquals(bucketName + '/' + itemId + "?uploads", httpRequest.uri());
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"POST\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + '\n' + bucketName + '/'
				+ itemId + "?uploads",
			canonicalReq
		);
	}

	@Test
	public void testMpuCompleteRequest()
	throws Exception {

		final String bucketName = "/bucket3";
		final long itemSize = 12345;
		final long partSize = 1234;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final CompositeDataIoTask<DataItem> mpuTask = new BasicCompositeDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0, partSize
		);

		// emulate the upload id setting
		mpuTask.put(KEY_UPLOAD_ID, "qazxswedc");
		// emulate the sub-tasks completion
		final List<? extends PartialDataIoTask<DataItem>> subTasks = mpuTask.getSubTasks();
		for(final PartialDataIoTask<DataItem> subTask : subTasks) {
			subTask.startRequest();
			subTask.finishRequest();
			subTask.startResponse();
			subTask.finishResponse();
		}
		assertTrue(mpuTask.allSubTasksDone());

		final HttpRequest httpRequest = getHttpRequest(mpuTask, storageNodeAddrs[0]);
		final HttpHeaders reqHeaders = httpRequest.headers();

		assertEquals(HttpMethod.POST, httpRequest.method());
		assertEquals(bucketName + '/' + itemId + "?uploadId=qazxswedc", httpRequest.uri());
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));
		final byte[] reqContent = ((FullHttpRequest) httpRequest).content().array();
		final int contentLen = reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH);
		assertTrue(contentLen > 0);
		assertEquals(contentLen, reqContent.length);

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"POST\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + '\n' + bucketName + '/'
				+ itemId + "?uploadId=qazxswedc",
			canonicalReq
		);
	}

	@Test
	public void testUploadPartRequest()
	throws Exception {

		final String bucketName = "/bucket4";
		final long itemSize = 12345;
		final long partSize = 1234;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final CompositeDataIoTask<DataItem> mpuTask = new BasicCompositeDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0, partSize
		);

		// emulate the upload id setting
		mpuTask.put(KEY_UPLOAD_ID, "vfrtgbnhy");

		final List<? extends PartialDataIoTask> subTasks = mpuTask.getSubTasks();
		final int subTasksCount = subTasks.size();
		assertEquals(itemSize / partSize + (itemSize % partSize > 0 ? 1 : 0), subTasksCount);
		PartialDataIoTask subTask;

		for(int i = 0; i < subTasksCount; i ++) {

			subTask = subTasks.get(i);
			final HttpRequest httpRequest = getHttpRequest(subTask, storageNodeAddrs[0]);
			assertEquals(HttpMethod.PUT, httpRequest.method());
			assertEquals(
				bucketName + '/' + itemId + "?partNumber=" + (i + 1) + "&uploadId=vfrtgbnhy",
				httpRequest.uri()
			);
			final HttpHeaders reqHeaders = httpRequest.headers();
			assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
			final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
				.parse(reqHeaders.get(HttpHeaderNames.DATE));
			assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
			final int contentLen = reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH);
			if(i == subTasksCount - 1) {
				assertEquals(itemSize % partSize, contentLen);
			} else {
				assertEquals(partSize, contentLen);
			}
			assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

			final String canonicalReq = getCanonical(
				reqHeaders, httpRequest.method(), httpRequest.uri()
			);
			assertEquals(
				"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + '\n'
					+ bucketName + '/' + itemId + "?partNumber=" + (i + 1) + "&uploadId=vfrtgbnhy",
				canonicalReq
			);
		}
	}

	@Test
	public void testReadRequest()
	throws Exception {

		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.READ, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.GET, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"GET\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testReadFixedRangesRequest()
	throws Exception {

		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final List<Range> fixedRanges = new ArrayList<>();
		fixedRanges.add(new Range(0, 0, -1));
		fixedRanges.add(new Range(1, 1, -1));
		fixedRanges.add(new Range(2, 2, -1));
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.READ, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), fixedRanges, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.GET, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertEquals("bytes=0-0,1-1,2-2", reqHeaders.get(HttpHeaderNames.RANGE));
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"GET\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testReadRandomRangesRequest()
	throws Exception {

		final int rndRangeCount = 2;
		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.READ, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, rndRangeCount
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.GET, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final String rangeHeaderValue = reqHeaders.get(HttpHeaderNames.RANGE);
		assertTrue(rangeHeaderValue.startsWith("bytes="));
		final List<Range> ranges = new ArrayList<>();
		for(final String nextRangeValue : rangeHeaderValue.substring(6).split(",")) {
			ranges.add(new Range(nextRangeValue));
		}
		assertEquals(rndRangeCount, ranges.size());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"GET\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testUpdateRequest()
	throws Exception {

		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.UPDATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.PUT, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(itemSize, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testUpdateFixedRangesRequest()
	throws Exception {

		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final List<Range> fixedRanges = new ArrayList<>();
		fixedRanges.add(new Range(0, 0, -1));
		fixedRanges.add(new Range(1, 1, -1));
		fixedRanges.add(new Range(2, 2, -1));
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.UPDATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), fixedRanges, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.PUT, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(3, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertEquals("bytes=0-0,1-1,2-2", reqHeaders.get(HttpHeaderNames.RANGE));
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testUpdateRandomRangesRequest()
	throws Exception {

		final int rndRangeCount = 2;
		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.UPDATE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, rndRangeCount
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.PUT, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertTrue(0 < reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(itemSize > reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		final String rangeHeaderValue = reqHeaders.get(HttpHeaderNames.RANGE);
		assertTrue(rangeHeaderValue.startsWith("bytes="));
		final List<Range> ranges = new ArrayList<>();
		for(final String nextRangeValue : rangeHeaderValue.substring(6).split(",")) {
			ranges.add(new Range(nextRangeValue));
		}
		assertEquals(rndRangeCount, ranges.size());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"PUT\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}

	@Test
	public void testDeleteRequest()
	throws Exception {

		final String bucketName = "/bucket2";
		final long itemSize = 10240;
		final String itemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			itemId, Long.parseLong(itemId, Character.MAX_RADIX), itemSize
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.DELETE, dataItem, null, bucketName,
			Credential.getInstance(UID, SECRET), null, 0
		);
		final HttpRequest httpRequest = getHttpRequest(ioTask, storageNodeAddrs[0]);

		assertEquals(HttpMethod.DELETE, httpRequest.method());
		assertEquals(bucketName + "/" + itemId, httpRequest.uri());
		final HttpHeaders reqHeaders = httpRequest.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final Date dateHeaderValue = DateUtil.FMT_DATE_RFC1123
			.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), dateHeaderValue.getTime(), 10_000);
		assertEquals(0, reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH).intValue());
		assertTrue(reqHeaders.get(HttpHeaderNames.AUTHORIZATION).startsWith("AWS " + UID + ":"));

		final String canonicalReq = getCanonical(
			reqHeaders, httpRequest.method(), httpRequest.uri()
		);
		assertEquals(
			"DELETE\n\n\n" + reqHeaders.get(HttpHeaderNames.DATE) + "\n" + bucketName + '/' + itemId,
			canonicalReq
		);
	}
}
