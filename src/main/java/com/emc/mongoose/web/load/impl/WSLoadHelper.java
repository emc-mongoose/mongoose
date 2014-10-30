package com.emc.mongoose.web.load.impl;
//
import com.codahale.metrics.MetricRegistry;
import com.emc.mongoose.base.load.StorageNodeExecutor;
import com.emc.mongoose.base.load.impl.LoadExecutorBase;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
//
import com.emc.mongoose.web.load.WSNodeExecutor;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
/**
 Created by kurila on 22.04.14.
 */
public class WSLoadHelper {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String KEY_NODE_ADDR = "node.addr",
								KEY_LOAD_NUM = "load.number",
								KEY_LOAD_TYPE = "load.type",
								KEY_API = "api";
	//
	public static CloseableHttpClient initClient(
		final int totalThreadCount, final int dataPageSize, final WSRequestConfig reqConf
	) {
		// create and configure the connection manager for HTTP client
		final PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setDefaultMaxPerRoute(totalThreadCount);
		connMgr.setMaxTotal(totalThreadCount);
		connMgr.setDefaultConnectionConfig(
			ConnectionConfig
				.custom()
				.setBufferSize(dataPageSize)
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
			.setMaxConnPerRoute(totalThreadCount)
			.setMaxConnTotal(totalThreadCount);
		if(!reqConf.getRetries()) {
			httpClientBuilder.disableAutomaticRetries();
		}
		//
		return httpClientBuilder.build();
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
				//Add thread context
				final Map<String,String> context = new HashMap<>();
				context.put(KEY_NODE_ADDR, addrs[i]);
				context.put(KEY_API,reqConf.getAPI());
				context.put(KEY_LOAD_TYPE,reqConf.getLoadType().toString());
				context.put(KEY_LOAD_NUM,String.valueOf(LoadExecutorBase.getLastInstanceNum()));
				//
				nextNodeExecutor = new BasicNodeExecutor<>(
					runTimeConfig, addrs[i], threadsPerNode, reqConf, parentMetrics, name, context
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
