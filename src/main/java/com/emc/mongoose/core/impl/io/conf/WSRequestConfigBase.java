package com.emc.mongoose.core.impl.io.conf;
// mongoose-common
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.request.SharedHeadersAdder;
import com.emc.mongoose.common.net.http.request.HostHeaderSetter;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.api.io.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.data.ContentSource;
// mongoose-core-impl
import static com.emc.mongoose.core.impl.item.data.BasicMutableDataItem.getRangeOffset;
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.data.BasicWSObject;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.commons.configuration.Configuration;
import org.apache.http.ExceptionLogger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
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
import org.apache.http.message.HeaderGroup;
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
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 Created by kurila on 09.06.14.
 */
public abstract class WSRequestConfigBase<T extends WSObject, C extends Container<T>>
extends RequestConfigBase<T, C>
implements WSRequestConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static long serialVersionUID = 42L;
	protected final String signMethod;
	protected boolean fsAccess, versioning, pipelining;
	protected SecretKeySpec secretKey;
	//
	private final HttpAsyncRequester client;
	private final ConnectingIOReactor ioReactor;
	private final BasicNIOConnPool connPool;
	private final Thread clientDaemon;
	//
	public static <T extends WSObject, C extends Container<T>> WSRequestConfig<T, C> getInstance() {
		return newInstanceFor(RunTimeConfig.getContext().getApiName());
	}
	//
	@SuppressWarnings("unchecked")
	public static <T extends WSObject, C extends Container<T>> WSRequestConfig<T, C> newInstanceFor(
		final String api
	) {
		final WSRequestConfig<T, C> reqConf;
		final String apiImplClsFQN = PACKAGE_IMPL_BASE + "." + api.toLowerCase() + "." + ADAPTER_CLS;
		try {
			final Class apiImplCls = Class.forName(apiImplClsFQN);
			final Constructor<WSRequestConfig<T, C>>
				constructor = (Constructor<WSRequestConfig<T, C>>) apiImplCls.getConstructors()[0];
			reqConf = constructor.<T, C>newInstance();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
			throw new RuntimeException(e);
		}
		return reqConf;
	}
	//
	protected HeaderGroup sharedHeaders = new HeaderGroup();
	protected static final ThreadLocal<Mac> THRLOC_MAC = new ThreadLocal<>();
	//
	public WSRequestConfigBase()
	throws NoSuchAlgorithmException, IOReactorException {
		this(null);
	}
	//
	@SuppressWarnings({"unchecked", "SynchronizeOnNonFinalField"})
	protected WSRequestConfigBase(final WSRequestConfigBase<T, C> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		signMethod = runTimeConfig.getHttpSignMethod();
		final Configuration customHeaders = runTimeConfig.getHttpCustomHeaders();
		if(customHeaders != null) {
			final Iterator<String> customHeadersIterator = customHeaders.getKeys();
			if(customHeadersIterator != null) {
				String nextKey, nextValue;
				while(customHeadersIterator.hasNext()) {
					nextKey = customHeadersIterator.next();
					nextValue = customHeaders.getString(nextKey);
					sharedHeaders.updateHeader(new BasicHeader(nextKey, nextValue));
				}
			}
		}
		sharedHeaders.updateHeader(
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
			.add(new SharedHeadersAdder(sharedHeaders))
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
			.setBufferSize((int) runTimeConfig.getIOBufferSizeMin())
			.build();
		final long timeOutMs = runTimeConfig.getLoadLimitTimeUnit().toMillis(
			runTimeConfig.getLoadLimitTimeValue()
		);
		final IOReactorConfig.Builder ioReactorConfigBuilder = IOReactorConfig
			.custom()
			.setIoThreadCount(1)
			.setBacklogSize((int) runTimeConfig.getSocketBindBackLogSize())
			.setInterestOpQueued(runTimeConfig.getSocketInterestOpQueued())
			.setSelectInterval(runTimeConfig.getSocketSelectInterval())
			.setShutdownGracePeriod(runTimeConfig.getSocketTimeOut())
			.setSoKeepAlive(runTimeConfig.getSocketKeepAliveFlag())
			.setSoLinger(runTimeConfig.getSocketLinger())
			.setSoReuseAddress(runTimeConfig.getSocketReuseAddrFlag())
			.setSoTimeout(runTimeConfig.getSocketTimeOut())
			.setTcpNoDelay(runTimeConfig.getSocketTCPNoDelayFlag())
			.setRcvBufSize((int) runTimeConfig.getIOBufferSizeMin())
			.setSndBufSize((int) runTimeConfig.getIOBufferSizeMin())
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
	throws URISyntaxException {
		final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
			getHttpMethod(), getDataUriPath(obj)
		);
		try {
			applyHostHeader(request, nodeAddr);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a host header");
		}
		switch(loadType) {
			case UPDATE:
			case APPEND:
				applyRangesHeaders(request, obj);
			case CREATE:
				applyPayLoad(request, obj);
				break;
			case READ:
			case DELETE:
				applyPayLoad(request, null);
				break;
		}
		applyHeadersFinally(request);
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
			case UPDATE:
				// TODO update container, toggle the versioning for example
				break;
			case APPEND:
				throw new IllegalStateException(
					"Append operation is not supported for the containers"
				);
			case CREATE:
				break;
			case READ:
				break;
			case DELETE:
				break;
		}
		applyHeadersFinally(request);
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
	public final WSRequestConfigBase<T, C> setAPI(final String api) {
		super.setAPI(api);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T, C> setContentSource(final ContentSource dataSrc) {
		super.setContentSource(dataSrc);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setUserName(final String userName) {
		super.setUserName(userName);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setNamePrefix(final String prefix) {
		super.setNamePrefix(prefix);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setNameRadix(final int radix) {
		super.setNameRadix(radix);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T, C> setLoadType(final IOTask.Type loadType) {
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
	public WSRequestConfigBase<T, C> setFileAccessEnabled(final boolean flag) {
		this.fsAccess = flag;
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setVersioning(final boolean flag) {
		this.versioning = flag;
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setPipelining(final boolean flag) {
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
	public WSRequestConfigBase<T, C> setRunTimeConfig(final RunTimeConfig runTimeConfig) {
		//
		try {
			setScheme(this.runTimeConfig.getStorageProto());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_STORAGE_SCHEME);
		}
		//
		try {
			setNameSpace(this.runTimeConfig.getStorageNameSpace());
		} catch(final NoSuchElementException e) {
			LOG.debug(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_STORAGE_NAMESPACE);
		} catch(final IllegalStateException e) {
			LOG.debug(Markers.ERR, "Failed to set the namespace", e);
		}
		//
		try {
			setFileAccessEnabled(runTimeConfig.getDataFileAccessEnabled());
		} catch(final NoSuchElementException e) {
			LOG.debug(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_DATA_FS_ACCESS);
		}
		//
		try {
			setVersioning(runTimeConfig.getDataVersioningEnabled());
		} catch(final NoSuchElementException e) {
			LOG.debug(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_DATA_FS_ACCESS);
		}
		//
		try {
			setPipelining(runTimeConfig.getHttpPipeliningFlag());
		} catch(final NoSuchElementException e) {
			LOG.debug(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_HTTP_PIPELINING);
		}
		//
		super.setRunTimeConfig(runTimeConfig);
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T, C> setSecret(final String secret) {
		super.setSecret(secret);
		try {
			secretKey = new SecretKeySpec(secret.getBytes(Constants.DEFAULT_ENC), signMethod);
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
		return (Class<T>) BasicWSObject.class;
	}
	//
	@Override
	public final HeaderGroup getSharedHeaders() {
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
		sharedHeaders = HeaderGroup.class.cast(in.readObject());
		LOG.trace(Markers.MSG, "Got headers set {}", sharedHeaders);
		setNameSpace(String.class.cast(in.readObject()));
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
			(verifyContentFlag && IOTask.Type.READ.equals(loadType)) || fsAccess
		) {
			dataItem.setName(Long.toString(dataItem.getOffset(), MutableDataItem.ID_RADIX));
		}*/
	}
	//
	@Override
	public void applyHeadersFinally(final HttpEntityEnclosingRequest httpRequest) {
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
			if(httpRequest.getClass().isInstance(HttpEntityEnclosingRequest.class)) {
				msgBuff
					.append("\tcontent: ")
					.append(SizeUtil.formatSize(httpRequest.getEntity().getContentLength()))
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
	protected final String getFilePathFor(final T dataItem) {
		if(fsAccess && namePrefix != null && !namePrefix.isEmpty()) {
			return "/" + namePrefix + "/" + dataItem.getName();
		} else {
			return "/" + dataItem.getName();
		}
	}
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
						sb.append(RunTimeConfig.LIST_SEP);
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
						sb.append(RunTimeConfig.LIST_SEP);
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
		httpRequest.setHeader(HttpHeaders.DATE, LowPrecisionDateGenerator.getDateText());
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
	protected void applyMetaDataHeaders(final HttpEntityEnclosingRequest httpRequest) {
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
		final byte sigData[];
		Mac mac = THRLOC_MAC.get();
		if(mac == null) {
			try {
				mac = Mac.getInstance(signMethod);
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
		if(fsAccess && namePrefix != null && !namePrefix.isEmpty()) {
			createDirectoryPath(storageNodeAddrs[0], namePrefix);
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
					tgtHost = new HttpHost(
						t[0], Integer.parseInt(t[1]), runTimeConfig.getStorageProto()
					);
				} catch(final Exception e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to determine the request target host"
					);
				}
			} else {
				tgtHost = new HttpHost(
					tgtAddr, runTimeConfig.getApiTypePort(runTimeConfig.getApiName()),
					runTimeConfig.getStorageProto()
				);
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
}
