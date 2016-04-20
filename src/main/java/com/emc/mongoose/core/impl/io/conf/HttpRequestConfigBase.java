package com.emc.mongoose.core.impl.io.conf;
// mongoose-common
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.value.async.AsyncCurrentDateInput;
import com.emc.mongoose.common.io.value.async.AsyncPatternDefinedInput;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.request.HostHeaderSetter;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.item.data.ContentSource;
// mongoose-core-impl
import static com.emc.mongoose.common.io.value.RangePatternDefinedInput.PATTERN_SYMBOL;
import static com.emc.mongoose.core.impl.item.data.BasicMutableDataItem.getRangeOffset;
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.data.BasicHttpData;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.commons.configuration.Configuration;
import org.apache.http.ExceptionLogger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
//
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
//
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 09.06.14.
 */
public abstract class HttpRequestConfigBase<T extends HttpDataItem, C extends Container<T>>
extends RequestConfigBase<T, C>
implements HttpRequestConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static long serialVersionUID = 42L;
	protected final static String SIGN_METHOD = "HmacSHA1";
	protected boolean fsAccess, versioning, pipelining;
	protected SecretKeySpec secretKey;
	//
	private final HttpAsyncRequester client;
	private final ConnectingIOReactor ioReactor;
	private final BasicNIOConnPool connPool;
	private final Thread clientDaemon;
	//
	public static <T extends HttpDataItem, C extends Container<T>> HttpRequestConfig<T, C> getInstance() {
		return newInstanceFor(BasicConfig.THREAD_CONTEXT.get().getStorageHttpApi());
	}
	//
	@SuppressWarnings("unchecked")
	public static <T extends HttpDataItem, C extends Container<T>> HttpRequestConfig<T, C> newInstanceFor(
		final String api
	) {
		final HttpRequestConfig<T, C> reqConf;
		final String apiImplClsFQN = PACKAGE_IMPL_BASE + "." + api.toLowerCase() + "." + ADAPTER_CLS;
		try {
			final Class apiImplCls = Class.forName(apiImplClsFQN);
			final Constructor<HttpRequestConfig<T, C>>
				constructor = (Constructor<HttpRequestConfig<T, C>>) apiImplCls.getConstructors()[0];
			reqConf = constructor.<T, C>newInstance();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
			throw new RuntimeException(e);
		}
		return reqConf;
	}
	//
	protected Map<String, Header> sharedHeaders = new HashMap<>();
	protected Map<String, Header> dynamicHeaders = new HashMap<>();
	protected static final ThreadLocal<Mac> THRLOC_MAC = new ThreadLocal<>();
	//
	public HttpRequestConfigBase()
	throws NoSuchAlgorithmException, IOReactorException {
		this(null);
	}
	//
	@SuppressWarnings({"unchecked", "SynchronizeOnNonFinalField"})
	protected HttpRequestConfigBase(final HttpRequestConfigBase<T, C> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		final Configuration customHeaders = appConfig.getStorageHttpHeaders();
		if(customHeaders != null) {
			final Iterator<String> customHeadersIterator = customHeaders.getKeys();
			if(customHeadersIterator != null) {
				String nextKey, nextValue;
				while(customHeadersIterator.hasNext()) {
					nextKey = customHeadersIterator.next();
					nextValue = customHeaders.getString(nextKey);
					if(-1 < nextKey.indexOf(PATTERN_SYMBOL)) {
						dynamicHeaders.put(nextKey, new BasicHeader(nextKey, nextValue));
					} else {
						sharedHeaders.put(nextKey, new BasicHeader(nextKey, nextValue));
					}
				}
			}
		}
		sharedHeaders.put(
			HttpHeaders.CONTENT_TYPE,
			new BasicHeader(
				HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.getMimeType()
			)
		);
		try {
			if(reqConf2Clone != null) {
				this.setSecret(reqConf2Clone.getSecret()).setScheme(reqConf2Clone.getScheme());
				this.setFileAccessEnabled(reqConf2Clone.getFileAccessEnabled());
				this.setPipelining(reqConf2Clone.getPipelining());
			}
			//
			final String pkgSpec = getClass().getPackage().getName();
			setAPI(pkgSpec.substring(pkgSpec.lastIndexOf('.') + 1));
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Request config instantiation failure");
		}
		// create HTTP client
		final HttpProcessor httpProcessor= HttpProcessorBuilder
			.create()
			.add(this)
			.add(new HostHeaderSetter())
			.add(new RequestConnControl())
			.add(new RequestContent(false))
			.build();
		client = new HttpAsyncRequester(
			httpProcessor, NoConnectionReuseStrategy.INSTANCE,
			new ExceptionLogger() {
				@Override
				public final void log(final Exception e) {
					LogUtil.exception(LOG, Level.DEBUG, e, "HTTP client internal failure");
				}
			}
		);
		//
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(appConfig.getIoBufferSizeMin())
			.build();
		final long timeOutMs = TimeUnit.SECONDS.toMillis(appConfig.getLoadLimitTime());
		final IOReactorConfig.Builder ioReactorConfigBuilder = IOReactorConfig
			.custom()
			.setIoThreadCount(1)
			.setBacklogSize(appConfig.getNetworkSocketBindBacklogSize())
			.setInterestOpQueued(appConfig.getNetworkSocketInterestOpQueued())
			.setSelectInterval(appConfig.getNetworkSocketSelectInterval())
			.setShutdownGracePeriod(appConfig.getNetworkSocketTimeoutMilliSec())
			.setSoKeepAlive(appConfig.getNetworkSocketKeepAlive())
			.setSoLinger(appConfig.getNetworkSocketLinger())
			.setSoReuseAddress(appConfig.getNetworkSocketReuseAddr())
			.setSoTimeout(appConfig.getNetworkSocketTimeoutMilliSec())
			.setTcpNoDelay(appConfig.getNetworkSocketTcpNoDelay())
			.setRcvBufSize(appConfig.getIoBufferSizeMin())
			.setSndBufSize(appConfig.getIoBufferSizeMin())
			.setConnectTimeout(
				timeOutMs > 0 && timeOutMs < Integer.MAX_VALUE ? (int) timeOutMs : Integer.MAX_VALUE
			);
		//
		final NHttpClientEventHandler reqExecutor = new HttpAsyncRequestExecutor();
		//
		final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
			reqExecutor, connConfig
		);
		//
		try {
			ioReactor = new DefaultConnectingIOReactor(
				ioReactorConfigBuilder.build(),
				new GroupThreadFactory("wsConfigWorker<" + toString() + ">", true)
			);
		} catch(final IOReactorException e) {
			throw new IllegalStateException("Failed to build the I/O reactor", e);
		}
		//
		final NIOConnFactory<HttpHost, NHttpClientConnection>
			connFactory = new BasicNIOConnFactory(connConfig);
		//
		connPool = new BasicNIOConnPool(
			ioReactor, connFactory,
			timeOutMs > 0 && timeOutMs < Integer.MAX_VALUE ? (int) timeOutMs : Integer.MAX_VALUE
		);
		connPool.setMaxTotal(1);
		connPool.setDefaultMaxPerRoute(1);
		clientDaemon = new Thread(
			new HttpClientRunTask(ioEventDispatch, ioReactor), "wsConfigDaemon<" + toString() + ">"
		);
		clientDaemon.setDaemon(true);
	}
	//
	@Override
	public HttpEntityEnclosingRequest createGenericRequest(final String method, final String uri) {
		return new BasicHttpEntityEnclosingRequest(method, uri);
	}
	//
	@Override
	public HttpEntityEnclosingRequest createDataRequest(final T obj, final String nodeAddr)
	throws URISyntaxException, IllegalArgumentException, IllegalStateException {
		final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
			getHttpMethod(), getDataUriPath(obj)
		);
		try {
			applyHostHeader(request, nodeAddr);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a host header");
		}
		switch(loadType) {
			case WRITE:
				if(obj.hasScheduledUpdates() || obj.hasBeenUpdated()) {
					applyRangesHeaders(request, obj);
				}
				applyPayLoad(request, obj);
				break;
			case READ:
			case DELETE:
				applyPayLoad(request, null);
				break;
		}
		return request;
	}
	//
	@Override
	public HttpEntityEnclosingRequest createContainerRequest(
		final C container, final String nodeAddr
	) throws URISyntaxException {
		final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
			getHttpMethod(), getContainerUriPath(container)
		);
		try {
			applyHostHeader(request, nodeAddr);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a host header");
		}
		switch(loadType) {
			case WRITE:
				break;
			case READ:
				break;
			case DELETE:
				break;
		}
		return request;
	}
	//
	@Override
	public String getHttpMethod() {
		switch(loadType) {
			case READ:
				return METHOD_GET;
			case DELETE:
				return METHOD_DELETE;
			default:
				return METHOD_PUT;
		}
	}
	//
	@Override
	public final HttpRequestConfigBase<T, C> setAPI(final String api) {
		super.setAPI(api);
		return this;
	}
	//
	@Override
	public final HttpRequestConfigBase<T, C> setContentSource(final ContentSource dataSrc) {
		super.setContentSource(dataSrc);
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setUserName(final String userName) {
		super.setUserName(userName);
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setNamePrefix(final String prefix) {
		super.setNamePrefix(prefix);
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setNameRadix(final int radix) {
		super.setNameRadix(radix);
		return this;
	}
	//
	@Override
	public final HttpRequestConfigBase<T, C> setLoadType(final LoadType loadType) {
		super.setLoadType(loadType);
		return this;
	}
	//
	@Override
	public final boolean getFileAccessEnabled() {
		return fsAccess;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setFileAccessEnabled(final boolean flag) {
		this.fsAccess = flag;
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setVersioning(final boolean flag) {
		this.versioning = flag;
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setPipelining(final boolean flag) {
		this.pipelining = flag;
		return this;
	}
	//
	@Override
	public boolean getPipelining() {
		return pipelining;
	}
	//
	@Override
	public final boolean getVersioning() {
		return versioning;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setAppConfig(final AppConfig appConfig) {
		// setScheme(...)
		setNameSpace(appConfig.getStorageHttpNamespace());
		setFileAccessEnabled(appConfig.getStorageHttpFsAccess());
		setVersioning(appConfig.getStorageHttpVersioning());
		final String containerName = appConfig.getItemContainerName();
		if(containerName != null && !containerName.isEmpty()) {
			setContainer((C) new BasicContainer<T>(containerName));
		} else {
			setContainer(null);
		}
		// setPipelining(false);
		super.setAppConfig(appConfig);
		//
		return this;
	}
	//
	@Override
	public HttpRequestConfigBase<T, C> setSecret(final String secret) {
		super.setSecret(secret);
		try {
			secretKey = secret == null || secret.isEmpty() ?
				null : new SecretKeySpec(secret.getBytes(Constants.DEFAULT_ENC), SIGN_METHOD);
		} catch(UnsupportedEncodingException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Configuration error");
		}
		return this;
	}
	//
	@Override
	@SuppressWarnings("unchecked")
	public Class getContainerClass() {
		return BasicContainer.class;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<T> getItemClass() {
		return (Class<T>) BasicHttpData.class;
	}
	//
	@Override
	public final Map<String, Header> getSharedHeaders() {
		return sharedHeaders;
	}
	//
	private final static ThreadLocal<Map<String, HttpHost>>
		THREAD_CACHED_NODE_MAP = new ThreadLocal<>();
	//
	@Override
	public final HttpHost getNodeHost(final String nodeAddr) {
		Map<String, HttpHost> cachedNodeMap = THREAD_CACHED_NODE_MAP.get();
		if(cachedNodeMap == null) {
			cachedNodeMap = new HashMap<>();
			THREAD_CACHED_NODE_MAP.set(cachedNodeMap);
		}
		//
		HttpHost nodeHost = cachedNodeMap.get(nodeAddr);
		if(nodeHost == null) {
			if(nodeAddr.contains(HOST_PORT_SEP)) {
				final String nodeAddrParts[] = nodeAddr.split(HOST_PORT_SEP);
				if(nodeAddrParts.length == 2) {
					nodeHost = new HttpHost(
						nodeAddrParts[0], Integer.valueOf(nodeAddrParts[1]), getScheme()
					);
				} else {
					LOG.fatal(Markers.ERR, "Invalid node address: {}", nodeAddr);
					nodeHost = null;
				}
			} else {
				nodeHost = new HttpHost(nodeAddr, getPort(), getScheme());
			}
			cachedNodeMap.put(nodeAddr, nodeHost);
		}
		//
		return nodeHost;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		sharedHeaders = (Map<String, Header>) in.readObject();
		dynamicHeaders = (Map<String, Header>) in.readObject();
		setNameSpace((String) in.readObject());
		setFileAccessEnabled(in.readBoolean());
		setVersioning(in.readBoolean());
		setPipelining(in.readBoolean());
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(sharedHeaders);
		out.writeObject(dynamicHeaders);
		out.writeObject(getNameSpace());
		out.writeBoolean(getFileAccessEnabled());
		out.writeBoolean(getVersioning());
		out.writeBoolean(getPipelining());
	}
	//
	protected void applyObjectId(final T dataItem, final HttpResponse argUsedToOverrideImpl) {
		/*final String oldOid = dataItem.getName();
		if(
			oldOid == null || oldOid.isEmpty() ||
			(verifyContentFlag && AppConfig.LoadType.READ.equals(loadType)) || fsAccess
		) {
			dataItem.setName(Long.toString(dataItem.getOffset(), MutableDataItem.ID_RADIX));
		}*/
	}
	//
	@Override
	public void applyHeadersFinally(final HttpRequest httpRequest) {
		try {
			applyDateHeader(httpRequest);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a date header");
		}
		try {
			applyMetaDataHeaders(httpRequest);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a metadata headers");
		}
		try {
			applyAuthHeader(httpRequest);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply an auth header");
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			final StringBuilder msgBuff = new StringBuilder("built request: ")
				.append(httpRequest.getRequestLine().getMethod()).append(' ')
				.append(httpRequest.getRequestLine().getUri()).append('\n');
			for(final Header header: httpRequest.getAllHeaders()) {
				msgBuff
					.append('\t').append(header.getName())
					.append(": ").append(header.getValue())
					.append('\n');
			}
			if(httpRequest instanceof HttpEntityEnclosingRequest) {
				final long contentLength = ((HttpEntityEnclosingRequest) httpRequest)
					.getEntity()
					.getContentLength();
				msgBuff
					.append("\tcontent: ")
					.append(SizeInBytes.formatFixedSize(contentLength))
					.append(" bytes");
			} else {
				msgBuff.append("\t---- no content ----");
			}
			LOG.trace(Markers.MSG, msgBuff.toString());
		}
	}
	//
	protected abstract String getDataUriPath(final T dataItem)
	throws IllegalArgumentException, URISyntaxException;
	//
	protected abstract String getContainerUriPath(final Container<T> container)
	throws IllegalArgumentException, URISyntaxException;
	//
	protected final void applyPayLoad(
		final HttpEntityEnclosingRequest httpRequest, final HttpEntity httpEntity
	) {
		httpRequest.setEntity(httpEntity);
	}
	//
	private final static ThreadLocal<StringBuilder> THRLOC_SB = new ThreadLocal<>();
	// merge subsequent updated ranges functionality is here
	protected final void applyRangesHeaders(final HttpRequest httpRequest, final T dataItem) {
		httpRequest.removeHeaders(HttpHeaders.RANGE); // cleanup
		//
		final int prefixLen = VALUE_RANGE_PREFIX.length();
		StringBuilder sb = THRLOC_SB.get();
		if(sb == null) {
			sb = new StringBuilder(VALUE_RANGE_PREFIX);
			THRLOC_SB.set(sb);
		} else {
			sb.setLength(prefixLen); // reset
		}
		//
		if(dataItem.isAppending()) {
			sb.append(dataItem.getSize()).append(VALUE_RANGE_CONCAT);
		} else if(dataItem.hasScheduledUpdates()) {
			final int rangeCount = dataItem.getCountRangesTotal();
			long nextRangeOffset;
			for(int i = 0; i < rangeCount; i ++) {
				if(dataItem.isCurrLayerRangeUpdating(i)) {
					if(sb.length() > prefixLen) {
						sb.append(',');
					}
					nextRangeOffset = getRangeOffset(i);
					sb
						.append(nextRangeOffset)
						.append(VALUE_RANGE_CONCAT)
						.append(nextRangeOffset + dataItem.getRangeSize(i) - 1);
				}
			}
			for(int i = 0; i < rangeCount; i ++) {
				if(dataItem.isNextLayerRangeUpdating(i)) {
					if(sb.length() > prefixLen) {
						sb.append(',');
					}
					nextRangeOffset = getRangeOffset(i);
					sb
						.append(nextRangeOffset)
						.append(VALUE_RANGE_CONCAT)
						.append(nextRangeOffset + dataItem.getRangeSize(i) - 1);
				}
			}
		} else {
			throw new IllegalStateException("no pending range updates/appends to apply");
		}
		//
		httpRequest.addHeader(HttpHeaders.RANGE, sb.toString());
	}
	/*
	protected final static DateFormat FMT_DATE_RFC1123 = new SimpleDateFormat(
		"EEE, dd MMM yyyy HH:mm:ss zzz", Main.LOCALE_DEFAULT
	) {
		{ setTimeZone(Main.TZ_UTC); }
	};*/
	//
	protected void applyDateHeader(final HttpRequest httpRequest) {
		httpRequest.setHeader(HttpHeaders.DATE, AsyncCurrentDateInput.INSTANCE.get());
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Apply date header \"{}\" to the request: \"{}\"",
				httpRequest.getLastHeader(HttpHeaders.DATE), httpRequest
			);
		}
	}
	//
	protected void applyHostHeader(final HttpRequest httpRequest, final String nodeAddr) {
		httpRequest.setHeader(HttpHeaders.HOST, getNodeHost(nodeAddr).toHostString());
	}
	//
	protected void applyMetaDataHeaders(final HttpRequest httpRequest) {
	}
	//
	protected abstract void applyAuthHeader(final HttpRequest httpRequest);
	//
	//@Override
	//public final int hashCode() {
	//	return uriBuilder.hashCode()^mac.hashCode()^api.hashCode();
	//}
	//
	@Override
	public String getSignature(final String canonicalForm) {
		//
		if(secretKey == null) {
			return null;
		}
		//
		final byte sigData[];
		Mac mac = THRLOC_MAC.get();
		if(mac == null) {
			try {
				mac = Mac.getInstance(SIGN_METHOD);
				mac.init(secretKey);
			} catch(final NoSuchAlgorithmException | InvalidKeyException e) {
				e.printStackTrace(System.out);
				throw new IllegalStateException("Failed to init MAC cypher instance");
			}
			THRLOC_MAC.set(mac);
		}
		sigData = mac.doFinal(canonicalForm.getBytes());
		return Base64.encodeBase64String(sigData);
	}
	//
	@Override
	public void applySuccResponseToObject(final HttpResponse response, final T dataItem) {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Got response with {} bytes of payload data",
				response.getFirstHeader(HttpHeaders.CONTENT_LENGTH).getValue()
			);
		}
		// may invoke applyObjectId in some implementations
	}
	//
	@Override
	public final void close()
	throws IOException {
		//
		try {
			super.close();
		} finally {
			clientDaemon.interrupt();
			LOG.debug(Markers.MSG, "Client thread \"{}\" stopped", clientDaemon);
		}
		//
		if(connPool != null && connPool.isShutdown()) {
			connPool.closeExpired();
			try {
				connPool.closeIdle(1, TimeUnit.MILLISECONDS);
			} finally {
				try {
					connPool.shutdown(1);
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Connection pool shutdown failure");
				}
			}
		}
		//
		ioReactor.shutdown(1);
		LOG.debug(Markers.MSG, "Closed web storage client");
	}
	//
	@Override
	public void configureStorage(final String storageNodeAddrs[])
	throws IllegalStateException {
		final String containerName = container.getName();
		int firstSepPos = containerName.indexOf(File.pathSeparatorChar);
		if(fsAccess && firstSepPos >= 0) {
			final String path = containerName.substring(firstSepPos);
			if(!path.isEmpty()) {
				createDirectoryPath(storageNodeAddrs[0], path);
			}
		}
	}
	//
	protected abstract void createDirectoryPath(final String node, final String path)
	throws IllegalStateException;
	//
	@Override
	public final HttpResponse execute(
		final String tgtAddr, final HttpRequest request, final long timeOut, final TimeUnit timeUnit
	) {
		//
		try {
			if(!clientDaemon.isAlive()) {
				clientDaemon.start();
			}
		} catch(final IllegalThreadStateException e) {
			LOG.error(
				Markers.ERR, "#{}: failed to start the client thread which is in state: {}",
				hashCode(), clientDaemon.getState()
			);
		}
		//
		HttpResponse response = null;
		//
		final HttpCoreContext ctx = new HttpCoreContext();
		HttpHost tgtHost = null;
		if(tgtAddr != null) {
			if(tgtAddr.contains(":")) {
				final String t[] = tgtAddr.split(":");
				try {
					tgtHost = new HttpHost(t[0], Integer.parseInt(t[1]), SCHEME);
				} catch(final Exception e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to determine the request target host"
					);
				}
			} else {
				tgtHost = new HttpHost(tgtAddr, appConfig.getStoragePort(), SCHEME);
			}
		} else {
			LOG.warn(Markers.ERR, "Failed to determine the 1st storage node address");
		}
		//
		if(tgtHost != null && connPool != null) {
			ctx.setTargetHost(tgtHost);
			//
			try {
				response = client.execute(
					new BasicAsyncRequestProducer(tgtHost, request),
					new BasicAsyncResponseConsumer(), connPool, ctx
				).get(timeOut, timeUnit);
			} catch(final TimeoutException e) {
				if(!isClosed()) {
					LOG.warn(Markers.ERR, "HTTP request timeout: {}", request.getRequestLine());
				}
			} catch(final InterruptedException e) {
				if(!isClosed()) {
					LOG.debug(Markers.ERR, "Interrupted during HTTP request execution");
				}
			} catch(final ExecutionException e) {
				if(!isClosed()) {
					LogUtil.exception(
						LOG, Level.WARN, e,
						"HTTP request \"{}\" execution failure @ \"{}\"", request, tgtHost
					);
				}
			}
		}
		//
		return response;
	}
	//
	private final static Map<String, Input<String>> HEADER_VALUE_INPUTS = new ConcurrentHashMap<>();
	/**
	Created by kurila on 30.01.15.
	*/
	@Override
	public final void process(final HttpRequest request, final HttpContext context)
	throws HttpException, IOException {
		// add all the shared headers if missing
		Header nextHeader;
		String headerValue;
		Input<String> headerValueInput;
		for(final String nextKey : sharedHeaders.keySet()) {
			nextHeader = sharedHeaders.get(nextKey);
			if(!request.containsHeader(nextKey)) {
				request.setHeader(nextHeader);
			}
		}
		//
		for(final String nextKey : dynamicHeaders.keySet()) {
			nextHeader = sharedHeaders.get(nextKey);
			headerValue = nextHeader.getValue();
			if(headerValue != null) {
				// header value is a generator pattern
				headerValueInput  = HEADER_VALUE_INPUTS.get(nextKey);
				// try to find the corresponding generator in the registry
				if(headerValueInput == null) {
					// create new generator and put it into the registry for reuse
					headerValueInput = new AsyncPatternDefinedInput(headerValue);
					// spin while header value generator is not ready
					while(null == (headerValue = headerValueInput.get())) {
						Thread.yield();
					}
					HEADER_VALUE_INPUTS.put(nextKey, headerValueInput);
				} else {
					headerValue = headerValueInput.get();
				}
				// put the generated header value into the request
				request.setHeader(new BasicHeader(nextKey, headerValue));
			}
		}
		// add all other required headers
		applyHeadersFinally(request);
	}
}
