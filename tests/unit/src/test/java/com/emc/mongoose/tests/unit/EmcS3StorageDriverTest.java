package com.emc.mongoose.tests.unit;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.data.BasicDataIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.BasicDataItem;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.http.emc.base.EmcConstants;
import com.emc.mongoose.storage.driver.net.http.emc.s3.EmcS3StorageDriver;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.storage.net.http.HttpConfig;

import com.github.akurilov.commons.collection.Range;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 06.10.17.
 */
public class EmcS3StorageDriverTest
extends EmcS3StorageDriver {

	private static final Credential CREDENTIAL = Credential.getInstance(
		"user1", "u5QtPuQx+W5nrrQQEg7nArBqSgC8qLiDt2RhQthb"
	);
	private static final String NS = "ns1";
	private static final ThreadLocal<MessageDigest>
		MD5_DIGEST = ThreadLocal.withInitial(DigestUtils::getMd5Digest);

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
			authConfig.setSecret(CREDENTIAL.getSecret());
			return config;
		} catch(final Throwable cause) {
			throw new RuntimeException(cause);
		}
	}

	public EmcS3StorageDriverTest()
	throws Exception {
		this(getConfig());
	}

	private EmcS3StorageDriverTest(final Config config)
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
		return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
	}

	@Test
	public void testConcatRequest()
	throws Exception {

		final String srcBucketName = "srcBucket";
		final long itemSize = 10240;
		final int srcItemsCount = 10;
		final List<DataItem> srcItems = new ArrayList<>(srcItemsCount);
		for(int i = 0; i < srcItemsCount; i ++) {
			final String srcItemId = "000000000000" + i;
			srcItems.add(
				new BasicDataItem(
					'/' + srcBucketName + '/' + srcItemId,
					Long.parseLong(srcItemId, Character.MAX_RADIX), itemSize
				)
			);
		}

		final String dstBucketName = "dstBucket";
		final String dstItemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			dstItemId, Long.parseLong(dstItemId, Character.MAX_RADIX), 0
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, '/' + dstBucketName, credential, null, 0,
			srcItems
		);

		final HttpRequest req = getHttpRequest(ioTask, storageNodeAddrs[0]);
		assertTrue(req instanceof FullHttpRequest);
		assertEquals(HttpMethod.PUT, req.method());
		assertEquals('/' + dstBucketName + '/' + dstItemId, req.uri());

		final HttpHeaders reqHeaders = req.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final int contentLen = reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH);
		final Date reqDate = DateUtil.FMT_DATE_RFC1123.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), reqDate.getTime(), 10_000);
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertEquals(
			HttpHeaderValues.APPLICATION_JSON.toString(),
			reqHeaders.get(HttpHeaderNames.CONTENT_TYPE)
		);
		assertEquals("true", reqHeaders.get(EmcConstants.KEY_X_EMC_MULTIPART_COPY));
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertTrue(
			reqHeaders
				.get(HttpHeaderNames.AUTHORIZATION)
				.startsWith("AWS " + credential.getUid() + ":")
		);
		final String contentMd5 = reqHeaders.get(HttpHeaderNames.CONTENT_MD5);

		final String canonicalReq = getCanonical(
			reqHeaders, req.method(), req.uri()
		);
		assertEquals(
			"PUT\n" + contentMd5 + '\n' + HttpHeaderValues.APPLICATION_JSON + '\n'
				+ reqHeaders.get(HttpHeaderNames.DATE) + '\n'
				+ EmcConstants.KEY_X_EMC_MULTIPART_COPY + ":true\n"
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ '/' + dstBucketName + '/' + dstItemId,
			canonicalReq
		);

		final byte[] contentBytes = ((FullHttpRequest) req).content().array();
		assertEquals(contentLen, contentBytes.length);
		final MessageDigest md5Digest = MD5_DIGEST.get();
		md5Digest.reset();
		final byte[] contentMd5Bytes = md5Digest.digest(contentBytes);
		final String contentMd5EncodedHash = new String(Base64.encodeBase64(contentMd5Bytes));
		assertEquals(contentMd5, contentMd5EncodedHash);

		final String contentStr = new String(contentBytes, StandardCharsets.US_ASCII);
		for(final DataItem srcItem : srcItems) {
			assertTrue(contentStr.contains("\"path\": \"" + srcItem.getName().substring(1) + "\""));
		}
	}

	@Test
	public void testConcatFixedRangesRequest()
	throws Exception {

		final String srcBucketName = "srcBucket";
		final long itemSize = 10240;
		final int srcItemsCount = 10;
		final List<DataItem> srcItems = new ArrayList<>(srcItemsCount);
		for(int i = 0; i < srcItemsCount; i ++) {
			final String srcItemId = "000000000000" + i;
			srcItems.add(
				new BasicDataItem(
					'/' + srcBucketName + '/' + srcItemId,
					Long.parseLong(srcItemId, Character.MAX_RADIX), itemSize
				)
			);
		}

		final List<Range> fixedRanges = new ArrayList<>(srcItemsCount);
		for(int i = 0; i < srcItemsCount; i ++) {
			fixedRanges.add(new Range(i * 1000, i * 1000 + 100, -1));
		}

		final String dstBucketName = "dstBucket";
		final String dstItemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			dstItemId, Long.parseLong(dstItemId, Character.MAX_RADIX), 0
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, '/' + dstBucketName, credential,
			fixedRanges, 0, srcItems
		);

		final HttpRequest req = getHttpRequest(ioTask, storageNodeAddrs[0]);
		assertTrue(req instanceof FullHttpRequest);
		assertEquals(HttpMethod.PUT, req.method());
		assertEquals('/' + dstBucketName + '/' + dstItemId, req.uri());

		final HttpHeaders reqHeaders = req.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final int contentLen = reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH);
		final Date reqDate = DateUtil.FMT_DATE_RFC1123.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), reqDate.getTime(), 10_000);
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertEquals(
			HttpHeaderValues.APPLICATION_JSON.toString(),
			reqHeaders.get(HttpHeaderNames.CONTENT_TYPE)
		);
		assertEquals("true", reqHeaders.get(EmcConstants.KEY_X_EMC_MULTIPART_COPY));
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertTrue(
			reqHeaders
				.get(HttpHeaderNames.AUTHORIZATION)
				.startsWith("AWS " + credential.getUid() + ":")
		);
		final String contentMd5 = reqHeaders.get(HttpHeaderNames.CONTENT_MD5);

		final String canonicalReq = getCanonical(
			reqHeaders, req.method(), req.uri()
		);
		assertEquals(
			"PUT\n" + contentMd5 + '\n' + HttpHeaderValues.APPLICATION_JSON + '\n'
				+ reqHeaders.get(HttpHeaderNames.DATE) + '\n'
				+ EmcConstants.KEY_X_EMC_MULTIPART_COPY + ":true\n"
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ '/' + dstBucketName + '/' + dstItemId,
			canonicalReq
		);

		final byte[] contentBytes = ((FullHttpRequest) req).content().array();
		assertEquals(contentLen, contentBytes.length);
		final MessageDigest md5Digest = MD5_DIGEST.get();
		md5Digest.reset();
		final byte[] contentMd5Bytes = md5Digest.digest(contentBytes);
		final String contentMd5EncodedHash = new String(Base64.encodeBase64(contentMd5Bytes));
		assertEquals(contentMd5, contentMd5EncodedHash);

		final String contentStr = new String(contentBytes, StandardCharsets.US_ASCII);
		final StringBuilder strb = new StringBuilder();
		rangeListToStringBuff(fixedRanges, dataItem.size(), strb);
		final Pattern rangesRegex = Pattern.compile("\"range\":\\s\"" + strb.toString() + "\"");
		final Matcher m = rangesRegex.matcher(contentStr);
		for(final DataItem srcItem : srcItems) {
			assertTrue(contentStr.contains("\"path\": \"" + srcItem.getName().substring(1) + "\""));
			assertTrue(m.find());
		}
	}

	@Test
	public void testConcatRandomRangesRequest()
	throws Exception {

		final String srcBucketName = "srcBucket";
		final long itemSize = 10240;
		final int srcItemsCount = 10;
		final List<DataItem> srcItems = new ArrayList<>(srcItemsCount);
		for(int i = 0; i < srcItemsCount; i ++) {
			final String srcItemId = "000000000000" + i;
			srcItems.add(
				new BasicDataItem(
					'/' + srcBucketName + '/' + srcItemId,
					Long.parseLong(srcItemId, Character.MAX_RADIX), itemSize
				)
			);
		}

		final String dstBucketName = "dstBucket";
		final String dstItemId = "00003brre8lgz";
		final DataItem dataItem = new BasicDataItem(
			dstItemId, Long.parseLong(dstItemId, Character.MAX_RADIX), 0
		);
		final DataIoTask<DataItem> ioTask = new BasicDataIoTask<>(
			hashCode(), IoType.CREATE, dataItem, null, '/' + dstBucketName, credential,
			null, srcItemsCount, srcItems
		);

		final HttpRequest req = getHttpRequest(ioTask, storageNodeAddrs[0]);
		assertTrue(req instanceof FullHttpRequest);
		assertEquals(HttpMethod.PUT, req.method());
		assertEquals('/' + dstBucketName + '/' + dstItemId, req.uri());

		final HttpHeaders reqHeaders = req.headers();
		assertEquals(storageNodeAddrs[0], reqHeaders.get(HttpHeaderNames.HOST));
		final int contentLen = reqHeaders.getInt(HttpHeaderNames.CONTENT_LENGTH);
		final Date reqDate = DateUtil.FMT_DATE_RFC1123.parse(reqHeaders.get(HttpHeaderNames.DATE));
		assertEquals(new Date().getTime(), reqDate.getTime(), 10_000);
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertEquals(
			HttpHeaderValues.APPLICATION_JSON.toString(),
			reqHeaders.get(HttpHeaderNames.CONTENT_TYPE)
		);
		assertEquals("true", reqHeaders.get(EmcConstants.KEY_X_EMC_MULTIPART_COPY));
		assertEquals(NS, reqHeaders.get(EmcConstants.KEY_X_EMC_NAMESPACE));
		assertTrue(
			reqHeaders
				.get(HttpHeaderNames.AUTHORIZATION)
				.startsWith("AWS " + credential.getUid() + ":")
		);
		final String contentMd5 = reqHeaders.get(HttpHeaderNames.CONTENT_MD5);

		final String canonicalReq = getCanonical(
			reqHeaders, req.method(), req.uri()
		);
		assertEquals(
			"PUT\n" + contentMd5 + '\n' + HttpHeaderValues.APPLICATION_JSON + '\n'
				+ reqHeaders.get(HttpHeaderNames.DATE) + '\n'
				+ EmcConstants.KEY_X_EMC_MULTIPART_COPY + ":true\n"
				+ EmcConstants.KEY_X_EMC_NAMESPACE + ':' + NS + '\n'
				+ '/' + dstBucketName + '/' + dstItemId,
			canonicalReq
		);

		final byte[] contentBytes = ((FullHttpRequest) req).content().array();
		assertEquals(contentLen, contentBytes.length);
		final MessageDigest md5Digest = MD5_DIGEST.get();
		md5Digest.reset();
		final byte[] contentMd5Bytes = md5Digest.digest(contentBytes);
		final String contentMd5EncodedHash = new String(Base64.encodeBase64(contentMd5Bytes));
		assertEquals(contentMd5, contentMd5EncodedHash);

		final String contentStr = new String(contentBytes, StandardCharsets.US_ASCII);
		final Pattern rangesRegex = Pattern.compile(
			"\"range\":\\s\"([0-9]+-[0-9]+,){9}[0-9]+-[0-9]+\""
		);
		final Matcher m = rangesRegex.matcher(contentStr);
		for(final DataItem srcItem : srcItems) {
			assertTrue(contentStr.contains("\"path\": \"" + srcItem.getName().substring(1) + "\""));
			assertTrue(m.find());
		}
	}
}
