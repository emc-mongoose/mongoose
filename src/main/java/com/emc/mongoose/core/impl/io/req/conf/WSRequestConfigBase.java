package com.emc.mongoose.core.impl.io.req.conf;
// mongoose-common
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.http.RequestSharedHeaders;
import com.emc.mongoose.common.http.RequestTargetHost;
import com.emc.mongoose.common.io.HTTPInputStream;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.src.DataSource;
// mongoose-core-impl
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.data.DataRanges;
import com.emc.mongoose.core.impl.io.req.WSRequestImpl;
import com.emc.mongoose.core.impl.io.task.BasicWSIOTask;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.commons.lang.text.StrBuilder;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
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
			LogUtil.failure(
				LOG, Level.FATAL, e,
				String.format("API implementation \"%s\" is not found", api)
			);
		} catch(final ClassCastException e) {
			LogUtil.failure(
				LOG, Level.FATAL, e,
				String.format(
					"Class \"%s\" is not valid API implementation for \"%s\"", apiImplClsFQN, api
				)
			);
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.FATAL, e, "WS API config instantiation failure");
		}
		return reqConf;
	}
	//
	protected HeaderGroup sharedHeaders = new HeaderGroup();
	protected Mac mac;
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
		mac = Mac.getInstance(signMethod);
		final String runName = runTimeConfig.getRunName(),
			runVersion = runTimeConfig.getRunVersion(),
			contentType = runTimeConfig.getHttpContentType();
		userAgent = runName + '/' + runVersion;
		//
		sharedHeaders.updateHeader(new BasicHeader(HttpHeaders.USER_AGENT, userAgent));
		sharedHeaders.updateHeader(new BasicHeader(HttpHeaders.CONNECTION, VALUE_KEEP_ALIVE));
		sharedHeaders.updateHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, contentType));
		try {
			if(reqConf2Clone!=null) {
				this.setSecret(reqConf2Clone.getSecret()).setScheme(reqConf2Clone.getScheme());
				this.setFileAccessEnabled(reqConf2Clone.getFileAccessEnabled());
			}
			//
			final String pkgSpec = getClass().getPackage().getName();
			setAPI(pkgSpec.substring(pkgSpec.lastIndexOf('.') + 1));
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Request config instantiation failure");
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
					LogUtil.failure(LOG, Level.DEBUG, e, "HTTP client internal failure");
				}
			}
		);
		//
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize((int)runTimeConfig.getDataBufferSize())
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
		return new WSRequestImpl(getHTTPMethod(), null, null);
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
		//
		super.setSecret(secret);
		//
		SecretKeySpec keySpec;
		try {
			keySpec = new SecretKeySpec(secret.getBytes(Constants.DEFAULT_ENC), signMethod);
			mac.init(keySpec);
		} catch(UnsupportedEncodingException e) {
			LOG.fatal(LogUtil.ERR, "Configuration error", e);
		} catch(InvalidKeyException e) {
			LOG.error(LogUtil.ERR, "Invalid secret key", e);
		}
		//
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
	@Override @SuppressWarnings("unchecked")
	public final WSIOTask<T> getRequestFor(final T dataItem, final String nodeAddr) {
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
			case CREATE:
				applyPayLoad(httpRequest, dataItem);
				break;
			case UPDATE:
				applyRangesHeaders(httpRequest, dataItem);
				applyPayLoad(httpRequest, dataItem.getPendingUpdatesContentEntity());
				break;
			case APPEND:
				applyAppendRangeHeader(httpRequest, dataItem);
				applyPayLoad(httpRequest, dataItem.getPendingAugmentContentEntity());
				break;
		}
	}
	//
	@Override
	public void applyHeadersFinally(final MutableWSRequest httpRequest) {
		try {
			applyDateHeader(httpRequest);
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to apply date header");
		}
		try {
			applyAuthHeader(httpRequest);
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to apply auth header");
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			final StrBuilder msgBuff = new StrBuilder("built request: ")
				.append(httpRequest.getRequestLine().getMethod()).append(' ')
				.append(httpRequest.getRequestLine().getUri()).appendNewLine();
			for(final Header header: httpRequest.getAllHeaders()) {
				msgBuff
					.append('\t').append(header.getName())
					.append(": ").append(header.getValue())
					.appendNewLine();
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
		long rangeBeg = -1, rangeEnd = -1, rangeLen;
		int rangeCount = dataItem.getCountRangesTotal();
		for(int i = 0; i < rangeCount; i++) {
			rangeLen = dataItem.getRangeSize(i);
			if(dataItem.isRangeUpdatePending(i)) {
				LOG.trace(LogUtil.MSG, "\"{}\": should update range #{}", dataItem, i);
				if(rangeBeg < 0) { // begin of the possible updated ranges sequence
					rangeBeg = DataRanges.getRangeOffset(i);
					rangeEnd = rangeBeg + rangeLen - 1;
					LOG.trace(
						LogUtil.MSG, "Begin of the possible updated ranges sequence @{}", rangeBeg
					);
				} else if(rangeEnd > 0) { // next range in the sequence of updated ranges
					rangeEnd += rangeLen;
				}
				if(i == rangeCount - 1) { // this is the last range which is updated also
					LOG.trace(LogUtil.MSG, "End of the updated ranges sequence @{}", rangeEnd);
					httpRequest.addHeader(
						HttpHeaders.RANGE, String.format(MSG_TMPL_RANGE_BYTES, rangeBeg, rangeEnd)
					);
				}
			} else if(rangeBeg > -1 && rangeEnd > -1) { // end of the updated ranges sequence
				LOG.trace(LogUtil.MSG, "End of the updated ranges sequence @{}", rangeEnd);
				httpRequest.addHeader(
					HttpHeaders.RANGE, String.format(MSG_TMPL_RANGE_BYTES, rangeBeg, rangeEnd)
				);
				// drop the updated ranges sequence info
				rangeBeg = -1;
				rangeEnd = -1;
			}
		}
	}
	//
	protected final void applyAppendRangeHeader(
		final MutableWSRequest httpRequest, final T dataItem
	) {
		httpRequest.addHeader(
				HttpHeaders.RANGE,
				String.format(MSG_TMPL_RANGE_BYTES_APPEND, dataItem.getSize())
		);
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
		synchronized(mac) {
			mac.reset();
			sigData = mac.doFinal(canonicalForm.getBytes());
		}
		return Base64.encodeBase64String(sigData);
	}
	//
	@Override
	public void receiveResponse(final HttpResponse response, final T dataItem) {
		// may invoke applyObjectId in some implementations
	}
	//
	@Override
	public final boolean consumeContent(final ContentDecoder in, final IOControl ioCtl, T dataItem) {
		boolean ok = true;
		try {
			if(dataItem != null) {
				if(loadType == IOTask.Type.READ) { // read
					if(verifyContentFlag) { // read and do verify
						try(
							final HTTPInputStream inStream = HTTPInputStream.getInstance(in, ioCtl)
						) {
							ok = dataItem.isContentEqualTo(inStream);
						} catch(final InterruptedException e) {
							// ignore
						}
					} else { // consume the whole data item content - may estimate the buffer size
						HTTPInputStream.consumeQuietly(in, ioCtl, buffSize);
					}
				}
			}
		} catch(final IOException e) {
			ok = false;
			if(isClosed()) {
				LogUtil.failure(
					LOG, Level.DEBUG, e, "Failed to read the content after closing"
				);
			} else {
				LogUtil.failure(LOG, Level.WARN, e, "Content reading failure");
			}
		} finally { // try to read the remaining data if left in the input stream
			HTTPInputStream.consumeQuietly(in, ioCtl, LoadExecutor.BUFF_SIZE_LO);
		}
		return ok;
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
				connPool.closeIdle(0, TimeUnit.MILLISECONDS);
			} finally {
				try {
					connPool.shutdown(0);
				} catch(final IOException e) {
					LogUtil.failure(
						LOG, Level.WARN, e, "Connection pool shutdown failure"
					);
				}
			}
		}
		//
		ioReactor.shutdown();
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
					LogUtil.failure(
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
					LogUtil.failure(
						LOG, Level.WARN, e,
						String.format(
							"HTTP request \"%s\" execution failure @ \"%s\"", request, tgtHost
						)
					);
				}
			}
		}
		//
		return response;
	}
}
