package com.emc.mongoose.storage.driver.net.http.amzs3;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.api.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.AUTH_PREFIX;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.COMPLETE_MPU_FOOTER;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.COMPLETE_MPU_HEADER;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.COMPLETE_MPU_PART_ETAG_END;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.COMPLETE_MPU_PART_NUM_END;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.COMPLETE_MPU_PART_NUM_START;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.HEADERS_CANONICAL;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.KEY_ATTR_UPLOAD_ID;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.KEY_UPLOAD_ID;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.KEY_X_AMZ_COPY_SOURCE;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.MAX_KEYS_LIMIT;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.PREFIX_KEY_X_AMZ;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.SIGN_METHOD;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.URL_ARG_VERSIONING;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.VERSIONING_ENABLE_CONTENT;
import static com.emc.mongoose.storage.driver.net.http.amzs3.AmzS3Api.VERSIONING_DISABLE_CONTENT;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.apache.logging.log4j.Level;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import org.xml.sax.SAXException;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 Created by kurila on 01.08.16.
 */
public class AmzS3StorageDriver<I extends Item, O extends IoTask<I>>
extends HttpStorageDriverBase<I, O> {
	
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final ThreadLocal<SAXParser> THREAD_LOCAL_XML_PARSER = new ThreadLocal<>();
	private static final ThreadLocal<StringBuilder>
		BUFF_CANONICAL = ThreadLocal.withInitial(StringBuilder::new),
		BUCKET_LIST_QUERY = ThreadLocal.withInitial(StringBuilder::new);
	
	private static final ThreadLocal<Map<String, Mac>> MAC_BY_SECRET = ThreadLocal.withInitial(
		HashMap::new
	);
	private static final Function<String, Mac> GET_MAC_BY_SECRET = secret -> {
		final SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(UTF_8), SIGN_METHOD);
		try {
			final Mac mac = Mac.getInstance(SIGN_METHOD);
			mac.init(secretKey);
			return mac;
		} catch(final NoSuchAlgorithmException | InvalidKeyException e) {
			throw new AssertionError(e);
		}
	};

	public AmzS3StorageDriver(
		final String jobName, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
		requestAuthTokenFunc = null; // do not use
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
	protected final String requestNewAuthToken(final Credential credential) {
		throw new AssertionError("Should not be invoked");
	}
	
	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {

		final int countLimit = count < 1 || count > MAX_KEYS_LIMIT ? MAX_KEYS_LIMIT : count;
		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();

		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());

		applyDynamicHeaders(reqHeaders);
		applySharedHeaders(reqHeaders);

		final StringBuilder queryBuilder = BUCKET_LIST_QUERY.get();
		queryBuilder.setLength(0);
		queryBuilder.append(path).append('?');
		if(prefix != null && !prefix.isEmpty()) {
			queryBuilder.append("prefix=").append(prefix);
		}
		if(lastPrevItem != null) {
			if('?' != queryBuilder.charAt(queryBuilder.length() - 1)) {
				queryBuilder.append('&');
			}
			String lastItemName = lastPrevItem.getName();
			if(lastItemName.contains("/")) {
				lastItemName = lastItemName.substring(lastItemName.lastIndexOf('/') + 1);
			}
			queryBuilder.append("marker=").append(lastItemName);
		}
		if('?' != queryBuilder.charAt(queryBuilder.length() - 1)) {
			queryBuilder.append('&');
		}
		queryBuilder.append("max-keys=").append(countLimit);
		final String query = queryBuilder.toString();

		applyAuthHeaders(reqHeaders, HttpMethod.GET, path, credential);

		final FullHttpRequest checkBucketReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.GET, query, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);
		final List<I> buff = new ArrayList<>(countLimit);
		final FullHttpResponse listResp;
		try {
			listResp = executeHttpRequest(checkBucketReq);
			final ByteBuf listRespContent = listResp.content();
			SAXParser listRespParser = THREAD_LOCAL_XML_PARSER.get();
			if(listRespParser == null) {
				listRespParser = SAXParserFactory.newInstance().newSAXParser();
				THREAD_LOCAL_XML_PARSER.set(listRespParser);
			} else {
				listRespParser.reset();
			}
			final BucketXmlListingHandler<I> listingHandler = new BucketXmlListingHandler<>(
				buff, path, itemFactory, idRadix
			);
			try(final InputStream contentStream = new ByteBufInputStream(listRespContent)) {
				listRespParser.parse(contentStream, listingHandler);
			}
			listRespContent.release();
			if(buff.size() == 0) {
				throw new EOFException();
			}
			if(!listingHandler.isTruncated()) {
				buff.add(null); // poison
			}
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final SAXException | ParserConfigurationException e) {
			LogUtil.exception(Level.WARN, e, "Failed to init the XML response parser");
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
		}

		return buff;
	}

	@Override
	protected HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {

		final HttpRequest httpRequest;
		final IoType ioType = ioTask.getIoType();

		if(ioTask instanceof CompositeDataIoTask) {
			if(IoType.CREATE.equals(ioType)) {
				final CompositeDataIoTask mpuTask = (CompositeDataIoTask) ioTask;
				if(mpuTask.allSubTasksDone()) {
					httpRequest = getCompleteMpuRequest(mpuTask, nodeAddr);
				} else { // this is the initial state of the task
					httpRequest = getInitMpuRequest(ioTask, nodeAddr);
				}
			} else {
				throw new AssertionError(
					"Non-create multipart operations are not implemented yet"
				);
			}
		} else if(ioTask instanceof PartialDataIoTask) {
			if(IoType.CREATE.equals(ioType)) {
				httpRequest = getUploadPartRequest((PartialDataIoTask) ioTask, nodeAddr);
			} else {
				throw new AssertionError(
					"Non-create multipart operations are not implemented yet"
				);
			}
		} else {
			httpRequest = super.getHttpRequest(ioTask, nodeAddr);
		}

		return httpRequest;
	}

	@Override
	protected final HttpMethod getTokenHttpMethod(final IoType ioType) {
		throw new AssertionError("Not implemented yet");
	}

	@Override
	protected final HttpMethod getPathHttpMethod(final IoType ioType) {
		switch(ioType) {
			case UPDATE:
				throw new AssertionError("Not implemnted yet");
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}

	@Override
	protected final String getTokenUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		throw new AssertionError("Not implemented");
	}

	@Override
	protected final String getPathUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		final String itemName = item.getName();
		if(itemName.startsWith("/")) {
			return itemName;
		} else {
			return "/" + itemName;
		}
	}

	@Override
	protected void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}

	private HttpRequest getInitMpuRequest(final O ioTask, final String nodeAddr) {
		final I item = ioTask.getItem();
		final String srcPath = ioTask.getSrcPath();
		if(srcPath != null && !srcPath.isEmpty()) {
			throw new AssertionError(
				"Multipart copy operation is not implemented yet"
			);
		}
		final String uriPath = getDataUriPath(item, srcPath, ioTask.getDstPath(), IoType.CREATE) +
			"?uploads";
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		if(nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		final HttpMethod httpMethod = HttpMethod.POST;
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, uriPath, httpHeaders
		);
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, ioTask.getCredential());
		return httpRequest;
	}

	private HttpRequest getUploadPartRequest(
		final PartialDataIoTask ioTask, final String nodeAddr
	) {
		final I item = (I) ioTask.getItem();

		final String srcPath = ioTask.getSrcPath();
		final String uriPath = getDataUriPath(item, srcPath, ioTask.getDstPath(), IoType.CREATE) +
			"?partNumber=" + (ioTask.getPartNumber() + 1) +
			"&uploadId=" + ioTask.getParent().get(KEY_UPLOAD_ID);

		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		if(nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
		final HttpMethod httpMethod = HttpMethod.PUT;
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, uriPath, httpHeaders
		);
		try {
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, ((DataItem) item).size());
		} catch(final IOException ignored) {
		}
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, ioTask.getCredential());
		return httpRequest;
	}

	private final static ThreadLocal<StringBuilder>
		THREAD_LOCAL_STRB = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	private FullHttpRequest getCompleteMpuRequest(
		final CompositeDataIoTask mpuTask, final String nodeAddr
	) {
		final StringBuilder content = THREAD_LOCAL_STRB.get();
		content.setLength(0);
		content.append(COMPLETE_MPU_HEADER);

		final List<PartialDataIoTask> subTasks = mpuTask.getSubTasks();
		int nextPartNum;
		String nextEtag;
		for(final PartialDataIoTask subTask : subTasks) {
			nextPartNum = subTask.getPartNumber() + 1;
			nextEtag = mpuTask.get(Integer.toString(nextPartNum));
			content
				.append(COMPLETE_MPU_PART_NUM_START)
				.append(nextPartNum)
				.append(COMPLETE_MPU_PART_NUM_END)
				.append(nextEtag)
				.append(COMPLETE_MPU_PART_ETAG_END);
		}
		content.append(COMPLETE_MPU_FOOTER);

		final String srcPath = mpuTask.getSrcPath();
		final I item = (I) mpuTask.getItem();
		final String uploadId = mpuTask.get(KEY_UPLOAD_ID);
		final String uriPath = getDataUriPath(item, srcPath, mpuTask.getDstPath(), IoType.CREATE) +
			"?uploadId=" + uploadId;

		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
		final HttpMethod httpMethod = HttpMethod.POST;
		final String contentStr = content.toString();
		final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
			HTTP_1_1, httpMethod, uriPath, Unpooled.wrappedBuffer(contentStr.getBytes()),
			httpHeaders, EmptyHttpHeaders.INSTANCE
		);
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, content.length());
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, mpuTask.getCredential());

		return httpRequest;
	}

	@Override
	public void complete(final Channel channel, final O ioTask) {
		if(channel != null && ioTask instanceof CompositeDataIoTask) {
			final CompositeDataIoTask compositeIoTask = (CompositeDataIoTask) ioTask;
			if(compositeIoTask.allSubTasksDone()) {
				Loggers.MULTIPART.info(
					"{},{},{}", compositeIoTask.getItem().getName(),
					compositeIoTask.get(KEY_UPLOAD_ID), compositeIoTask.getLatency()
				);
			} else {
				final String uploadId = channel.attr(KEY_ATTR_UPLOAD_ID).get();
				if(uploadId == null) {
					ioTask.setStatus(IoTask.Status.RESP_FAIL_NOT_FOUND);
				} else {
					// multipart upload has been initialized as a result of this I/O task
					compositeIoTask.put(KEY_UPLOAD_ID, uploadId);
				}
			}
		}
		super.complete(channel, ioTask);
	}

	@Override
	protected final void appendHandlers(final ChannelPipeline pipeline) {
		super.appendHandlers(pipeline);
		pipeline.addLast(new AmzS3ResponseHandler<>(this, verifyFlag));
	}

	@Override
	public final void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException {
		httpHeaders.set(KEY_X_AMZ_COPY_SOURCE, srcPath);
	}
	
	@Override
	protected final void applyAuthHeaders(
		final HttpHeaders httpHeaders, final HttpMethod httpMethod, final String dstUriPath,
		final Credential credential
	) {
		final String uid;
		final String secret;
		if(credential != null) {
			uid = credential.getUid();
			secret = credential.getSecret();
		} else if(this.credential != null) {
			uid = this.credential.getUid();
			secret = this.credential.getSecret();
		} else {
			return;
		}
		
		if(uid == null || secret == null) {
			return;
		}
		final Mac mac = MAC_BY_SECRET.get().computeIfAbsent(secret, GET_MAC_BY_SECRET);
		final String canonicalForm = getCanonical(httpHeaders, httpMethod, dstUriPath);
		final byte sigData[] = mac.doFinal(canonicalForm.getBytes());
		httpHeaders.set(
			HttpHeaderNames.AUTHORIZATION,
			AUTH_PREFIX + uid + ':' + BASE64_ENCODER.encodeToString(sigData)
		);
	}
	
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
				if(headerName.startsWith(PREFIX_KEY_X_AMZ)) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Map.Entry<String, String> header : httpHeaders) {
			headerName = header.getKey().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_X_AMZ)) {
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
		return String.format(super.toString(), "amzs3");
	}
}
