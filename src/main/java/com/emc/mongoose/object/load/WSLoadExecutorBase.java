package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.object.api.ObjectRequestConfig;
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
	protected final void initClient(
		final int totalThreadCount, final ObjectRequestConfig<T> objReqConf
	) throws ClassCastException {
		final WSRequestConfig<T> reqConf = (WSRequestConfig<T>) objReqConf;
		// create and configure the connection manager for HTTP client
		final PoolingHttpClientConnectionManager
			connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setDefaultMaxPerRoute(threadsPerNode);
		connMgr.setMaxTotal(totalThreadCount);
		connMgr.setDefaultConnectionConfig(
			ConnectionConfig
				.custom()
				.setBufferSize(runTimeConfig.getDataPageSize())
				.build()
		);
		// set shared headers to client builder
		final LinkedList<Header> headers = new LinkedList<>();
		final Map<String, String> sharedHeadersMap = reqConf.getSharedHeadersMap();
		for(final String key : sharedHeadersMap.keySet()) {
			headers.add(new BasicHeader(key, sharedHeadersMap.get(key)));
		}
		// configure and create the HTTP client
		final HttpClientBuilder
			httpClientBuilder = HttpClientBuilder
			.create()
			.setConnectionManager(connMgr)
			.setDefaultHeaders(headers)
			.setRetryHandler(reqConf.getRetryHandler())
			.disableCookieManagement()
			.setUserAgent(reqConf.getUserAgent())
			.setMaxConnPerRoute(threadsPerNode)
			.setMaxConnTotal(totalThreadCount);
		if(!reqConf.getRetries()) {
			httpClientBuilder.disableAutomaticRetries();
		}
		final CloseableHttpClient httpClient = httpClientBuilder.build();
		//
		reqConf.setClient(httpClient);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final void setFileBasedProducer(final String listFile) {
		// if path specified use the file as producer
		if(listFile!=null && listFile.length() > 0) {
			try {
				producer = (Producer<T>) new FileProducer<>(listFile, WSObjectImpl.class);
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
	protected final void initNodeExecutors(
		final String addrs[], final ObjectRequestConfig<T> objReqConf
	) throws ClassCastException {
		final WSRequestConfig<T> reqConf = (WSRequestConfig<T>) objReqConf;
		WSNodeExecutorImpl<T> nodeExecutor;
		for(int i = 0; i < addrs.length; i ++) {
			try {
				nodeExecutor = new WSNodeExecutorImpl<>(
					runTimeConfig, addrs[i], threadsPerNode, reqConf, metrics, getName()
				);
				nodes[i] = nodeExecutor;
			} catch(final CloneNotSupportedException e) {
				ExceptionHandler.trace(
					LOG, Level.FATAL, e, "Failed to clone the request configuration"
				);
			}
		}
		//
	}
}
