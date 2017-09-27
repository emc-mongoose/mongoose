package com.emc.mongoose.storage.driver.net.http.emc.s3;

import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.storage.driver.base.AsyncCurrentDateSupplier;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3StorageDriver;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.HEADERS_CANONICAL;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.PREFIX_KEY_X_AMZ;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.URL_ARG_VERSIONING;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.VERSIONING_DISABLE_CONTENT;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.VERSIONING_ENABLE_CONTENT;
import static com.emc.mongoose.storage.driver.net.http.emc.base.EmcConstants.KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED;
import static com.emc.mongoose.storage.driver.net.http.emc.base.EmcConstants.KEY_X_EMC_MULTIPART_COPY;
import static com.emc.mongoose.storage.driver.net.http.emc.base.EmcConstants.KEY_X_EMC_NAMESPACE;
import static com.emc.mongoose.storage.driver.net.http.emc.base.EmcConstants.PREFIX_KEY_X_EMC;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.math.Random;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

/**
 Created by andrey on 26.08.17.
 */
public class EmcS3StorageDriver<I extends Item, O extends IoTask<I>>
extends AmzS3StorageDriver<I, O> {

	private static final ThreadLocal<StringBuilder>
		BUFF_CANONICAL = ThreadLocal.withInitial(StringBuilder::new);
	private static final ThreadLocal<MessageDigest>
		MD5_DIGEST = ThreadLocal.withInitial(DigestUtils::getMd5Digest);

	private final Random rnd = new Random();

	public EmcS3StorageDriver(
		final String jobName, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException, InterruptedException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
	}

	@Override
	protected String requestNewPath(final String path) {

		// check the destination bucket if it exists w/ HEAD request
		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
		applyDynamicHeaders(reqHeaders);
		applySharedHeaders(reqHeaders);

		final Credential credential = pathToCredMap.getOrDefault(path, this.credential);
		applyAuthHeaders(reqHeaders, HttpMethod.HEAD, path, credential);
		final FullHttpRequest checkBucketReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.HEAD, path, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);
		final FullHttpResponse checkBucketResp;
		try {
			checkBucketResp = executeHttpRequest(checkBucketReq);
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}

		boolean bucketExistedBefore = true;
		if(checkBucketResp != null) {
			if(HttpResponseStatus.NOT_FOUND.equals(checkBucketResp.status())) {
				bucketExistedBefore = false;
			} else if(!HttpStatusClass.SUCCESS.equals(checkBucketResp.status().codeClass())) {
				Loggers.ERR.warn(
					"The bucket checking response is: {}", checkBucketResp.status().toString()
				);
			}
			checkBucketResp.release();
		}

		// create the destination bucket if it doesn't exists
		if(!bucketExistedBefore) {
			if(fsAccess) {
				reqHeaders.add(
					KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED, Boolean.toString(true)
				);
			}
			applyMetaDataHeaders(reqHeaders);
			applyAuthHeaders(reqHeaders, HttpMethod.PUT, path, credential);
			final FullHttpRequest putBucketReq = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.PUT, path, Unpooled.EMPTY_BUFFER, reqHeaders,
				EmptyHttpHeaders.INSTANCE
			);
			final FullHttpResponse putBucketResp;
			try {
				putBucketResp = executeHttpRequest(putBucketReq);
			} catch(final InterruptedException e) {
				throw new CancellationException();
			} catch(final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
				return null;
			}

			if(!HttpStatusClass.SUCCESS.equals(putBucketResp.status().codeClass())) {
				Loggers.ERR.warn(
					"The bucket creating response is: {}", putBucketResp.status().toString()
				);
				return null;
			}
			putBucketResp.release();
			if(fsAccess) {
				reqHeaders.remove(KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED);
			}
		}

		// check the bucket versioning state
		final String bucketVersioningReqUri = path + "?" + URL_ARG_VERSIONING;
		applyAuthHeaders(reqHeaders, HttpMethod.GET, bucketVersioningReqUri, credential);
		final FullHttpRequest getBucketVersioningReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.GET, bucketVersioningReqUri, Unpooled.EMPTY_BUFFER,
			reqHeaders, EmptyHttpHeaders.INSTANCE
		);
		final FullHttpResponse getBucketVersioningResp;
		try {
			getBucketVersioningResp = executeHttpRequest(getBucketVersioningReq);
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}

		final boolean versioningEnabled;
		if(!HttpStatusClass.SUCCESS.equals(getBucketVersioningResp.status().codeClass())) {
			Loggers.ERR.warn(
				"The bucket versioning checking response is: {}",
				getBucketVersioningResp.status().toString()
			);
			return null;
		} else {
			final String content = getBucketVersioningResp
				.content()
				.toString(StandardCharsets.US_ASCII);
			versioningEnabled = content.contains("Enabled");
		}
		getBucketVersioningResp.release();

		final FullHttpRequest putBucketVersioningReq;
		if(!versioning && versioningEnabled) {
			// disable bucket versioning
			reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, VERSIONING_DISABLE_CONTENT.length);
			applyAuthHeaders(reqHeaders, HttpMethod.PUT, bucketVersioningReqUri, credential);
			putBucketVersioningReq = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.PUT, bucketVersioningReqUri,
				Unpooled.wrappedBuffer(VERSIONING_DISABLE_CONTENT).retain(), reqHeaders,
				EmptyHttpHeaders.INSTANCE
			);
			final FullHttpResponse putBucketVersioningResp;
			try {
				putBucketVersioningResp = executeHttpRequest(putBucketVersioningReq);
			} catch(final InterruptedException e) {
				throw new CancellationException();
			} catch(final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
				return null;
			}

			if(!HttpStatusClass.SUCCESS.equals(putBucketVersioningResp.status().codeClass())) {
				Loggers.ERR.warn("The bucket versioning setting response is: {}",
					putBucketVersioningResp.status().toString()
				);
				putBucketVersioningResp.release();
				return null;
			}
			putBucketVersioningResp.release();
		} else if(versioning && !versioningEnabled) {
			// enable bucket versioning
			reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, VERSIONING_ENABLE_CONTENT.length);
			applyAuthHeaders(reqHeaders, HttpMethod.PUT, bucketVersioningReqUri, credential);
			putBucketVersioningReq = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.PUT, bucketVersioningReqUri,
				Unpooled.wrappedBuffer(VERSIONING_ENABLE_CONTENT).retain(), reqHeaders,
				EmptyHttpHeaders.INSTANCE
			);
			final FullHttpResponse putBucketVersioningResp;
			try {
				putBucketVersioningResp = executeHttpRequest(putBucketVersioningReq);
			} catch(final InterruptedException e) {
				throw new CancellationException();
			} catch(final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
				return null;
			}

			if(!HttpStatusClass.SUCCESS.equals(putBucketVersioningResp.status().codeClass())) {
				Loggers.ERR.warn("The bucket versioning setting response is: {}",
					putBucketVersioningResp.status().toString()
				);
				putBucketVersioningResp.release();
				return null;
			}
			putBucketVersioningResp.release();
		}

		return path;
	}

	@Override
	protected void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
		if(namespace != null && !namespace.isEmpty()) {
			httpHeaders.set(KEY_X_EMC_NAMESPACE, namespace);
		}
	}

	@Override
	protected String getCanonical(
		final HttpHeaders httpHeaders, final HttpMethod httpMethod, final String dstUriPath
	) {
		final StringBuilder buffCanonical = BUFF_CANONICAL.get();
		buffCanonical.setLength(0); // reset/clear
		buffCanonical.append(httpMethod.name());

		for(final AsciiString headerName : HEADERS_CANONICAL) {
			if(httpHeaders.contains(headerName)) {
				for(final String headerValue: httpHeaders.getAll(headerName)) {
					buffCanonical.append('\n').append(headerValue);
				}
			} else if(sharedHeaders != null && sharedHeaders.contains(headerName)) {
				buffCanonical.append('\n').append(sharedHeaders.get(headerName));
			} else {
				buffCanonical.append('\n');
			}
		}

		// x-amz-*, x-emc-*
		String headerName;
		Map<String, String> sortedHeaders = new TreeMap<>();
		if(sharedHeaders != null) {
			for(final Map.Entry<String, String> header : sharedHeaders) {
				headerName = header.getKey().toLowerCase();
				if(
					headerName.startsWith(PREFIX_KEY_X_AMZ) ||
						headerName.startsWith(PREFIX_KEY_X_EMC)
				) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Map.Entry<String, String> header : httpHeaders) {
			headerName = header.getKey().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_X_AMZ) || headerName.startsWith(PREFIX_KEY_X_EMC)) {
				sortedHeaders.put(headerName, header.getValue());
			}
		}
		for(final Map.Entry<String, String> sortedHeader : sortedHeaders.entrySet()) {
			buffCanonical
				.append('\n').append(sortedHeader.getKey())
				.append(':').append(sortedHeader.getValue());
		}

		buffCanonical.append('\n');
		buffCanonical.append(dstUriPath);

		if(Loggers.MSG.isTraceEnabled()) {
			Loggers.MSG.trace("Canonical representation:\n{}", buffCanonical);
		}

		return buffCanonical.toString();
	}

	@Override
	public String toString() {
		return super.toString().replace("amzs3", "emcs3");
	}

	@Override
	protected HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {
		if(IoType.CREATE.equals(ioTask.getIoType()) && ioTask instanceof DataIoTask) {
			final DataIoTask dataIoTask = (DataIoTask) ioTask;
			final List<DataItem> srcItemsToConcat = dataIoTask.getSrcItemsToConcat();
			if(srcItemsToConcat != null) {
				return getCopyRangesRequest(dataIoTask, nodeAddr, srcItemsToConcat);
			} else {
				return super.getHttpRequest(ioTask, nodeAddr);
			}
		} else {
			return super.getHttpRequest(ioTask, nodeAddr);
		}
	}

	// ECS/S3 Copy Ranges API support
	private final static ThreadLocal<StringBuilder>
		THREAD_LOCAL_STRB = ThreadLocal.withInitial(StringBuilder::new);

	private FullHttpRequest getCopyRangesRequest(
		final DataIoTask dataIoTask, final String nodeAddr, final List<DataItem> srcItemsToConcat
	) {
		final List<Range> fixedRanges = dataIoTask.getFixedRanges();
		final int randomRangesCount = dataIoTask.getRandomRangesCount();
		DataItem srcItem;
		String srcItemPath;

		// request content
		final StringBuilder content = THREAD_LOCAL_STRB.get();
		content.setLength(0);
		content
			.append("{\n\t\"content_type\": \"application/octet-stream\",\n\t\"segments\": [\n");
		if(fixedRanges == null || fixedRanges.isEmpty()) {
			if(randomRangesCount > 0) {
				long nextSrcItemSize, srcItemCellSize, srcItemCellStartPos, rangeStart, rangeEnd;
				try {
					for(int i = 0; i < srcItemsToConcat.size(); i ++) {
						srcItem = srcItemsToConcat.get(i);
						srcItemPath = srcItem.getName();
						if(srcItemPath.charAt(0) == '/') {
							srcItemPath = srcItemPath.substring(1);
						}
						content
							.append("\t\t{\n\t\t\t\"path\": \"")
							.append(srcItemPath)
							.append("\",\n");
						nextSrcItemSize = srcItem.size();
						srcItemCellSize = nextSrcItemSize / randomRangesCount;
						if(srcItemCellSize > 0) {
							content.append("\t\t\t\"range\": \"");
							for(int j = 0; j < randomRangesCount; j ++) {
								srcItemCellStartPos = j * srcItemCellSize;
								rangeStart = srcItemCellStartPos
									+ rnd.nextLong(srcItemCellSize / 2);
								rangeEnd = rangeStart
									+ rnd.nextLong(
										srcItemCellStartPos + srcItemCellSize - rangeStart - 1
									);
								content.append(rangeStart).append('-').append(rangeEnd);
								if(j < randomRangesCount - 1) {
									content.append(',');
								}
							}
							content.append("\"\n\t\t}");
						}
						if(i < srcItemsToConcat.size() - 1) {
							content.append(',');
						}
						content.append('\n');
					}
				} catch(final IOException ignored) {
				}
			} else {
				for(int i = 0; i < srcItemsToConcat.size(); i ++) {
					srcItem = srcItemsToConcat.get(i);
					content
						.append("\t\t{\n\t\t\t\"path\": \"")
						.append(srcItem.getName())
						.append("\"\n\t\t}");
					if(i < srcItemsToConcat.size() - 1) {
						content.append(',');
					}
					content.append('\n');
				}
			}
		} else {
			try {
				for(int i = 0; i < srcItemsToConcat.size(); i ++) {
					srcItem = srcItemsToConcat.get(i);
					content
						.append("\t\t{\n\t\t\t\"path\": \"")
						.append(srcItem.getName())
						.append("\",\n")
						.append("\t\t\t\"range\": \"");
					rangeListToStringBuff(fixedRanges, srcItem.size(), content);
					content.append("\"\n\t\t}");
					if(i < srcItemsToConcat.size() - 1) {
						content.append(',');
					}
					content.append('\n');
				}
			} catch(final IOException ignored) {
			}
		}
		content.append("\t]\n}\n");

		// request headers
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, content.length());
		httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
		httpHeaders.set(KEY_X_EMC_MULTIPART_COPY, "true");
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);

		// request content and its hash (required)
		final byte[] contentBytes = content.toString().getBytes();
		final MessageDigest md5Digest = MD5_DIGEST.get();
		md5Digest.reset();
		final byte[] contentMd5Bytes = md5Digest.digest(contentBytes);
		final String contentMd5EncodedHash = new String(Base64.encodeBase64(contentMd5Bytes));
		httpHeaders.set(HttpHeaderNames.CONTENT_MD5, contentMd5EncodedHash);

		final HttpMethod httpMethod = HttpMethod.PUT;
		final String uriPath = getDataUriPath(
			(I) dataIoTask.getItem(), dataIoTask.getSrcPath(), dataIoTask.getDstPath(),
			IoType.CREATE
		);
		final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
			HTTP_1_1, httpMethod, uriPath, Unpooled.wrappedBuffer(contentBytes),
			httpHeaders, EmptyHttpHeaders.INSTANCE
		);

		// remaining request headers
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, dataIoTask.getCredential());

		return httpRequest;
	}
}
