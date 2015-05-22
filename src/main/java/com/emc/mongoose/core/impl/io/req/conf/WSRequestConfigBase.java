package com.emc.mongoose.core.impl.io.req.conf;
// mongoose-common
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.http.RequestSharedHeaders;
import com.emc.mongoose.common.http.RequestTargetHost;
import com.emc.mongoose.common.io.HTTPContentDecoderChannel;
import com.emc.mongoose.common.io.StreamUtils;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.src.DataSource;
// mongoose-core-impl
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.data.RangeLayerData;
import com.emc.mongoose.core.impl.io.req.BasicWSRequest;
import com.emc.mongoose.core.impl.io.task.BasicWSIOTask;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.commons.codec.binary.Base64;
//
//
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
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestUserAgent;
//
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 09.06.14.
 */
public abstract class WSRequestConfigBase<T extends WSObject>
extends ObjectRequestConfigBase<T>
implements WSRequestConfig<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static long serialVersionUID = 42L;
	protected final String userAgent, signMethod;
	protected boolean fsAccess;
	protected SecretKeySpec secretKey;
	//
	private final HttpAsyncRequester client;
	private final ConnectingIOReactor ioReactor;
	private final BasicNIOConnPool connPool;
	private final Thread clientDaemon;
	//
	public static WSRequestConfigBase getInstance() {
		return newInstanceFor(RunTimeConfig.getContext().getApiName());
	}
	//
	private final static String
		FMT_CLS_PATH_ADAPTER_IMPL = "com.emc.mongoose.storage.adapter.%s.WSRequestConfigImpl";
	//
	@SuppressWarnings("unchecked")
	public static WSRequestConfigBase newInstanceFor(final String api) {
		WSRequestConfigBase reqConf = null;
		final String apiImplClsFQN = String.format(FMT_CLS_PATH_ADAPTER_IMPL, api.toLowerCase());
		try {
			final Class apiImplCls = Class.forName(apiImplClsFQN);
			final Constructor<WSRequestConfigBase>
				constructor = (Constructor<WSRequestConfigBase>) apiImplCls.getConstructors()[0];
			reqConf = constructor.newInstance();
		} catch(final ClassNotFoundException e) {
			LogUtil.exception(LOG, Level.FATAL, e, "API implementation \"{}\" is not found", api);
		} catch(final ClassCastException e) {
			LogUtil.exception(
				LOG, Level.FATAL, e,
				"Class \"{}\" is not valid API implementation for \"{}\"", apiImplClsFQN, api
			);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.FATAL, e, "WS API config instantiation failure");
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
	protected WSRequestConfigBase(final WSRequestConfig<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		signMethod = runTimeConfig.getHttpSignMethod();
		final String runName = runTimeConfig.getRunName(),
			runVersion = runTimeConfig.getRunVersion();
		userAgent = runName + '/' + runVersion;
		//
		sharedHeaders.updateHeader(new BasicHeader(HttpHeaders.USER_AGENT, userAgent));
		sharedHeaders.updateHeader(new BasicHeader(HttpHeaders.CONNECTION, VALUE_KEEP_ALIVE));
		sharedHeaders.updateHeader(
			new BasicHeader(
				HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.getMimeType()
			)
		);
		try {
			if(reqConf2Clone!=null) {
				this.setSecret(reqConf2Clone.getSecret()).setScheme(reqConf2Clone.getScheme());
				this.setFileAccessEnabled(reqConf2Clone.getFileAccessEnabled());
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
			.add(new RequestSharedHeaders(sharedHeaders))
			.add(new RequestTargetHost())
			.add(new RequestConnControl())
			.add(new RequestUserAgent(userAgent))
				//.add(new RequestExpectContinue(true))
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
			.setBufferSize((int) runTimeConfig.getDataBufferSize())
			.build();
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
			.setRcvBufSize((int) runTimeConfig.getDataBufferSize())
			.setSndBufSize((int) runTimeConfig.getDataBufferSize())
			.setConnectTimeout(runTimeConfig.getConnTimeOut());
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
				new NamingWorkerFactory(String.format("WSConfigurator<%s>-%d", toString(), hashCode()))
			);
		} catch(final IOReactorException e) {
			throw new IllegalStateException("Failed to build the I/O reactor", e);
		}
		//
		final NIOConnFactory<HttpHost, NHttpClientConnection>
			connFactory = new BasicNIOConnFactory(connConfig);
		//
		connPool = new BasicNIOConnPool(
			ioReactor, connFactory, runTimeConfig.getConnPoolTimeOut()
		);
		connPool.setMaxTotal(1);
		connPool.setDefaultMaxPerRoute(1);
		clientDaemon = new Thread(
			new HttpClientRunTask(ioEventDispatch, ioReactor),
			String.format("%s-WSConfigThread-%d", toString(), hashCode())
		);
		clientDaemon.setDaemon(true);
	}
	//
	@Override
	public final MutableWSRequest createRequest() {
		return new BasicWSRequest(getHTTPMethod(), null, null);
	}
	//
	@Override
	public MutableWSRequest.HTTPMethod getHTTPMethod() {
		MutableWSRequest.HTTPMethod method;
		switch(loadType) {
			case READ:
				method = MutableWSRequest.HTTPMethod.GET;
				break;
			case DELETE:
				method = MutableWSRequest.HTTPMethod.DELETE;
				break;
			default:
				method = MutableWSRequest.HTTPMethod.PUT;
				break;
		}
		return method;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setAPI(final String api) {
		super.setAPI(api);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setDataSource(final DataSource dataSrc) {
		super.setDataSource(dataSrc);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setUserName(final String userName) {
		super.setUserName(userName);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setRetries(final boolean retryFlag) {
		super.setRetries(retryFlag);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setLoadType(final IOTask.Type loadType) {
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
	public WSRequestConfigBase<T> setFileAccessEnabled(final boolean flag) {
		this.fsAccess = flag;
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setProperties(final RunTimeConfig runTimeConfig) {
		//
		try {
			setScheme(this.runTimeConfig.getStorageProto());
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_STORAGE_SCHEME);
		}
		//
		try {
			setNameSpace(this.runTimeConfig.getStorageNameSpace());
		} catch(final NoSuchElementException e) {
			LOG.debug(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_STORAGE_NAMESPACE);
		} catch(final IllegalStateException e) {
			LOG.debug(LogUtil.ERR, "Failed to set the namespace", e);
		}
		//
		try {
			setFileAccessEnabled(runTimeConfig.getStorageFileAccessEnabled());
		} catch(final NoSuchElementException e) {
			LOG.debug(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_STORAGE_FS_ACCESS);
		}
		//
		super.setProperties(runTimeConfig);
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setSecret(final String secret) {
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
	public final HeaderGroup getSharedHeaders() {
		return sharedHeaders;
	}
	//
	@Override
	public final String getUserAgent() {
		return userAgent;
	}
	//
	private final static Map<String, HttpHost> HTTP_HOST_CACHE = new ConcurrentHashMap<>();
	//
	@Override
	public HttpHost getHttpHost(final String addr) {
		final HttpHost httpHost;
		if(HTTP_HOST_CACHE.containsKey(addr)) {
			httpHost = HTTP_HOST_CACHE.get(addr);
		} else if(addr != null) {
			if(addr.contains(HOST_PORT_SEP)) {
				final String nodeAddrParts[] = addr.split(HOST_PORT_SEP);
				if(nodeAddrParts.length == 2) {
					httpHost = new HttpHost(
						nodeAddrParts[0], Integer.valueOf(nodeAddrParts[1]), getScheme()
					);
				} else {
					LOG.fatal(LogUtil.ERR, "Invalid node address: {}", addr);
					httpHost = null;
				}
			} else {
				httpHost = new HttpHost(addr, getPort(), getScheme());
			}
			HTTP_HOST_CACHE.put(addr, httpHost);
		} else {
			httpHost = null;
		}
		return httpHost;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final BasicWSIOTask<T> getRequestFor(final T dataItem, final String nodeAddr) {
		return BasicWSIOTask.getInstanceFor(this, dataItem, nodeAddr);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		sharedHeaders = HeaderGroup.class.cast(in.readObject());
		LOG.trace(LogUtil.MSG, "Got headers set {}", sharedHeaders);
		setNameSpace(String.class.cast(in.readObject()));
		setFileAccessEnabled(Boolean.class.cast(in.readObject()));
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(sharedHeaders);
		out.writeObject(getNameSpace());
		out.writeObject(getFileAccessEnabled());
	}
	//
	protected void applyObjectId(final T dataItem, final HttpResponse httpResponse) {
		dataItem.setId(Long.toString(dataItem.getOffset(), DataObject.ID_RADIX));
	}
	//
	@Override
	public void applyDataItem(final MutableWSRequest httpRequest, final T dataItem)
	throws IllegalStateException, URISyntaxException {
		applyObjectId(dataItem, null);
		applyURI(httpRequest, dataItem);
		switch(loadType) {
			case UPDATE:
			case APPEND:
				applyRangesHeaders(httpRequest, dataItem);
			case CREATE:
				applyPayLoad(httpRequest, dataItem);
				break;
			case READ:
			case DELETE:
				applyPayLoad(httpRequest, null);
				break;
		}
	}
	//
	@Override
	public void applyHeadersFinally(final MutableWSRequest httpRequest) {
		try {
			applyDateHeader(httpRequest);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply date header");
		}
		try {
			applyAuthHeader(httpRequest);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply auth header");
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
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
			LOG.trace(LogUtil.MSG, msgBuff.toString());
		}
	}
	//
	protected abstract void applyURI(final MutableWSRequest httpRequest, final T dataItem)
	throws IllegalArgumentException, URISyntaxException;
	//
	protected final void applyPayLoad(
		final MutableWSRequest httpRequest, final HttpEntity httpEntity
	) {
		httpRequest.setEntity(httpEntity);
	}
	// merge subsequent updated ranges functionality is here
	protected final void applyRangesHeaders(final MutableWSRequest httpRequest, final T dataItem) {
		httpRequest.removeHeaders(HttpHeaders.RANGE); // cleanup
		if(dataItem.isAppending()) {
			httpRequest.addHeader(HttpHeaders.RANGE, "bytes=" + dataItem.getSize() + "-");
		} else if(dataItem.hasUpdatedRanges()) {
			long rangeBeg = -1, rangeEnd = -1, rangeLen;
			int rangeCount = dataItem.getCountRangesTotal();
			for(int i = 0; i < rangeCount; i++) {
				rangeLen = dataItem.getRangeSize(i);
				if(dataItem.isRangeUpdatePending(i)) {
					LOG.trace(LogUtil.MSG, "\"{}\": should update range #{}", dataItem, i);
					if(rangeBeg < 0) { // begin of the possible updated ranges sequence
						rangeBeg = RangeLayerData.getRangeOffset(i);
						rangeEnd = rangeBeg + rangeLen - 1;
						LOG.trace(
							LogUtil.MSG, "Begin of the possible updated ranges sequence @{}", rangeBeg
						);
					} else if(rangeEnd > 0) { // next range in the sequence of updated ranges
						rangeEnd += rangeLen;
					}
					if(i == rangeCount - 1) { // this is the last range which is updated also
						LOG.trace(LogUtil.MSG, "End of the updated ranges sequence @{}", rangeEnd);
						httpRequest.addHeader(HttpHeaders.RANGE, "bytes=" + rangeBeg + "-" + rangeEnd);
					}
				} else if(rangeBeg > -1 && rangeEnd > -1) { // end of the updated ranges sequence
					LOG.trace(LogUtil.MSG, "End of the updated ranges sequence @{}", rangeEnd);
					httpRequest.addHeader(HttpHeaders.RANGE, "bytes=" + rangeBeg + "-" + rangeEnd);
					// drop the updated ranges sequence info
					rangeBeg = -1;
					rangeEnd = -1;
				}
			}
		} else {
			throw new IllegalStateException("applyRangesHeaders invoked while there's nothing to do");
		}
	}
	/*
	protected final static DateFormat FMT_DATE_RFC1123 = new SimpleDateFormat(
		"EEE, dd MMM yyyy HH:mm:ss zzz", Main.LOCALE_DEFAULT
	) {
		{ setTimeZone(Main.TZ_UTC); }
	};*/
	//
	protected void applyDateHeader(final MutableWSRequest httpRequest) {
		httpRequest.setHeader(HttpHeaders.DATE, LowPrecisionDateGenerator.getDateText());
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(
				LogUtil.MSG, "Apply date header \"{}\" to the request: \"{}\"",
				httpRequest.getLastHeader(HttpHeaders.DATE), httpRequest
			);
		}
	}
	//
	protected abstract void applyAuthHeader(final MutableWSRequest httpRequest);
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
				LogUtil.exception(LOG, Level.FATAL, e, "Failed to calculate the signature");
				throw new IllegalStateException("Failed to init MAC cypher instance");
			}
			THRLOC_MAC.set(mac);
		} else {
			mac.reset();
		}
		sigData = mac.doFinal(canonicalForm.getBytes());
		return Base64.encodeBase64String(sigData);
	}
	//
	@Override
	public void receiveResponse(final HttpResponse response, final T dataItem) {
		// may invoke applyObjectId in some implementations
	}
	//
	private final static ThreadLocal<ByteBuffer>
		THRLOC_BB_RESP_WRITE = new ThreadLocal<>(),
		THRLOC_BB_RESP_READ = new ThreadLocal<>();
	//
	@Override
	public final boolean consumeContent(final ContentDecoder in, final IOControl ioCtl, T dataItem) {
		boolean verifyPass = true;
		try {
			if(dataItem != null) {
				if(loadType == IOTask.Type.READ) { // read
					if(verifyContentFlag) { // read and do verify
						try(
							final ReadableByteChannel
								chanIn = HTTPContentDecoderChannel.getInstance(in)
						) {
							verifyPass = dataItem.equals(chanIn);
						}
					} else { // consume the whole data item content - may estimate the buffer size
						ByteBuffer bbuff = THRLOC_BB_RESP_READ.get();
						final long dataSize = dataItem.getSize();
						// should I adapt the buffer size?
						if(bbuff == null || bbuff.capacity() > 2 * dataSize || dataSize > 2 * bbuff.capacity()) {
							if(dataSize < LoadExecutor.BUFF_SIZE_LO) {
								bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_LO);
							} else if(dataSize > LoadExecutor.BUFF_SIZE_HI) {
								bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_HI);
							} else {
								bbuff = ByteBuffer.allocate((int) dataSize); // cast is safe
							}
							THRLOC_BB_RESP_READ.set(bbuff);
						} else {
							bbuff.clear();
						}
						StreamUtils.consumeQuietly(in, ioCtl, bbuff);
					}
				}
			}
		} catch(final IOException e) {
			verifyPass = false;
			if(isClosed()) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to read the content after closing");
			} else {
				LogUtil.exception(LOG, Level.WARN, e, "Content reading failure");
			}
		} finally { // try to read the remaining data if left in the input stream
			ByteBuffer bbuff = THRLOC_BB_RESP_WRITE.get();
			if(bbuff == null) {
				bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_LO);
				THRLOC_BB_RESP_WRITE.set(bbuff);
			} else {
				bbuff.clear();
			}
			StreamUtils.consumeQuietly(in, ioCtl, bbuff);
		}
		return verifyPass;
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
			LOG.debug(LogUtil.MSG, "Client thread \"{}\" stopped", clientDaemon);
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
		LOG.debug(LogUtil.MSG, "Closed web storage client");
	}
	//
	@Override
	public final HttpResponse execute(final String tgtAddr, final HttpRequest request)
	throws IllegalThreadStateException {
		//
		if(!clientDaemon.isAlive()) {
			clientDaemon.start();
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
			LOG.warn(LogUtil.ERR, "Failed to determine the 1st storage node address");
		}
		//
		if(tgtHost != null && connPool != null) {
			ctx.setTargetHost(tgtHost);
			//
			try {
				response = client.execute(
					new BasicAsyncRequestProducer(tgtHost, request),
					new BasicAsyncResponseConsumer(), connPool, ctx
				).get();
			} catch(final InterruptedException e) {
				if(!isClosed()) {
					LOG.debug(LogUtil.ERR, "Interrupted during HTTP request execution");
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
