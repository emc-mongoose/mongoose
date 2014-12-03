package com.emc.mongoose.web.load.impl;
//
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.base.load.StorageNodeExecutor;
import com.emc.mongoose.web.api.WSClient;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.api.impl.WSAsyncClientImpl;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.web.load.WSNodeExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
/**
 Created by kurila on 22.04.14.
 */
public class WSLoadHelper {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public static WSClient initClient(
		final int totalThreadCount, final RunTimeConfig runTimeConfig, final WSRequestConfig reqConf
	) {
		final WSClient client = new WSAsyncClientImpl(
			totalThreadCount, reqConf.getSharedHeaders(), reqConf.getUserAgent()
		);
		reqConf.setClient(client);
		return client;
		/* create and configure the connection manager for HTTP client
		final PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setDefaultMaxPerRoute(totalThreadCount);
		connMgr.setMaxTotal(totalThreadCount);
		connMgr.setDefaultConnectionConfig(
			ConnectionConfig
				.custom()
				.setBufferSize((int) runTimeConfig.getDataPageSize())
				.build()
		);
		// set shared headers to client builder
		// configure and create the HTTP client
		final HttpClientBuilder
			httpClientBuilder = HttpClientBuilder
			.create()
			.setConnectionManager(connMgr)
			.setDefaultHeaders(headers)
			.setRetryHandler(reqConf.getRetryHandler())
			.disableCookieManagement()
			.setUserAgent(reqConf.getUserAgent())
			.setMaxConnPerRoute(totalThreadCount)
			.setMaxConnTotal(totalThreadCount)
			.setDefaultRequestConfig(
				RequestConfig
					.custom()
					.setConnectionRequestTimeout(runTimeConfig.getConnPoolTimeOut())
					.setConnectTimeout(runTimeConfig.getConnTimeOut())
					.setSocketTimeout(runTimeConfig.getSocketTimeOut())
					.setStaleConnectionCheckEnabled(runTimeConfig.getStaleConnCheckFlag())
					.build()
			)
			.setDefaultSocketConfig(
				SocketConfig
					.custom()
					.setSoReuseAddress(runTimeConfig.getSocketReuseAddrFlag())
					.setSoKeepAlive(runTimeConfig.getSocketKeepAliveFlag())
					.setTcpNoDelay(runTimeConfig.getSocketTCPNoDelayFlag())
					.build()
			);
		if(!reqConf.getRetries()) {
			httpClientBuilder.disableAutomaticRetries();
		}
		//
		final CloseableHttpClient httpClient = httpClientBuilder.build();
		reqConf.setClient(httpClient);
		return httpClientBuilder.build();*/
	}
	//
	public static void initNodeExecutors(
		final String addrs[], final RunTimeConfig runTimeConfig,
		final WSRequestConfig<WSObject> reqConf, final int threadsPerNode,
		final MetricRegistry parentMetrics, final String name,
		final StorageNodeExecutor[] nodes
	) {
		WSNodeExecutor nextNodeExecutor;
		for(int i = 0; i < addrs.length; i ++) {
			try {
				nextNodeExecutor = new BasicNodeExecutor<>(
					runTimeConfig, addrs[i], threadsPerNode, reqConf, parentMetrics, name
				);
				nodes[i] = nextNodeExecutor;
			} catch(final CloneNotSupportedException e) {
				ExceptionHandler.trace(
					LOG, Level.FATAL, e, "Failed to clone the request configuration"
				);
			}
		}
	}
}
