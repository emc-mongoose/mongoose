package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.Header;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
/**
 Created by kurila on 22.04.14.
 */
public abstract class WSLoadExecutorBase<T extends WSObjectImpl>
extends ObjectLoadExecutorBase<T>
implements WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private volatile PoolingHttpClientConnectionManager connMgr;
	//
	protected WSLoadExecutorBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) throws ClassCastException {
		super(runTimeConfig, addrs, reqConf, maxCount, threadsPerNode, listFile);
	}
	//
	@Override
	protected final void initClient(final String addrs[], final RequestConfig<T> reqConf) {
		final int totalThreadCount = addrs.length * threadsPerNode;
		// create and configure the connection manager for HTTP client
		connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setDefaultMaxPerRoute(totalThreadCount);
		connMgr.setMaxTotal(totalThreadCount);
		connMgr.setDefaultConnectionConfig(
			ConnectionConfig
				.custom()
				.setBufferSize((int) runTimeConfig.getDataPageSize())
				.build()
		);
		// set shared headers to client builder
		final LinkedList<Header> headers = new LinkedList<>();
		final WSRequestConfig<T> wsReqConf = (WSRequestConfig<T>) reqConf;
		final Map<String, String> sharedHeadersMap = wsReqConf.getSharedHeadersMap();
		for(final String key : sharedHeadersMap.keySet()) {
			headers.add(new BasicHeader(key, sharedHeadersMap.get(key)));
		}
		// configure and create the HTTP client
		final HttpClientBuilder
			httpClientBuilder = HttpClientBuilder
			.create()
			.setConnectionManager(connMgr)
			.setDefaultHeaders(headers)
			.setRetryHandler(wsReqConf.getRetryHandler())
			.disableCookieManagement()
			.setUserAgent(wsReqConf.getUserAgent())
			.setMaxConnPerRoute(totalThreadCount)
			.setMaxConnTotal(totalThreadCount);
		if(!reqConf.getRetries()) {
			httpClientBuilder.disableAutomaticRetries();
		}
		final CloseableHttpClient httpClient = httpClientBuilder.build();
		//
		wsReqConf.setClient(httpClient);
	}
	//
	@Override
	protected final void initNodeExecutors(final String addrs[], final RequestConfig<T> reqConf) {
		WSNodeExecutorImpl<T> nodeExecutor;
		for(int i = 0; i < addrs.length; i ++) {
			try {
				nodeExecutor = new WSNodeExecutorImpl<>(
					runTimeConfig, addrs[i], threadsPerNode, (WSRequestConfig<T>) reqConf, metrics,
					getName()
				);
				nodes[i] = nodeExecutor;
			} catch(final CloneNotSupportedException e) {
				ExceptionHandler.trace(
					LOG, Level.FATAL, e, "Failed to clone the request configuration"
				);
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final void setFileBasedProducer(final String listFile) {
		// if path specified use the file as producer
		if(listFile != null && listFile.length() > 0) {
			try {
				producer = (Producer<T>) new FileProducer<>(listFile, WSObjectImpl.class);
				producer.setConsumer(this);
			} catch(final NoSuchMethodException e) {
				LOG.fatal(Markers.ERR, "Failed to get the constructor", e);
			} catch(final IOException e) {
				LOG.warn(Markers.ERR, "Failed to use object list file \"{}\"for reading", listFile);
				LOG.debug(Markers.ERR, e.toString(), e.getCause());
			}
		}
	}
	//
	@Override
	protected void logMetrics(final Marker logMarker) {
		super.logMetrics(logMarker);
		if(connMgr != null) {
			LOG.debug(Markers.PERF_AVG, connMgr.getTotalStats());
		}
	}
}
