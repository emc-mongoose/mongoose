package com.emc.mongoose.object.http;
/**
 Created by kurila on 22.04.14.
 */
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.Consumer;
import com.emc.mongoose.LoadExecutor;
import com.emc.mongoose.Producer;
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.data.persist.FileProducer;
import com.emc.mongoose.data.persist.LogConsumer;
import com.emc.mongoose.logging.ExceptionHandler;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.api.WSRequestConfig;
//
import com.emc.mongoose.remote.ServiceUtils;
import com.emc.mongoose.threading.RejectedTaskHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//
public abstract class WSLoadExecutor
extends Thread
implements LoadExecutor<WSObject> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final int threadsPerNode;
	private final ThreadPoolExecutor submitExecutor;
	private final WSNodeExecutor nodes[];
	private final PoolingHttpClientConnectionManager connMgr;
	private final CloseableHttpClient httpClient;
	private final UniformDataSource dataSrc;
	// METRICS section BEGIN
	private final MetricRegistry metrics = new MetricRegistry();
	private final Counter counterSubm, counterReqSucc, counterReqFail;
	private final Meter reqBytes;
	private final Histogram reqDur;
	//
	private final static MBeanServer MBEAN_SERVER = ServiceUtils.getMBeanServer(
		RunTimeConfig.getInt("remote.monitor.port")
	);
	protected final JmxReporter metricsReporter = JmxReporter.forRegistry(metrics)
		.convertDurationsTo(TimeUnit.SECONDS)
		.convertRatesTo(TimeUnit.SECONDS)
		.registerWith(MBEAN_SERVER)
		.build();
	// METRICS section END
	protected volatile Producer<WSObject> producer = null;
	protected volatile Consumer<WSObject> consumer;
	private volatile static int instanceN = 0;
	protected volatile long maxCount;
	//
	protected WSLoadExecutor(
		final String[] addrs, final WSRequestConfig reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		final int nodeCount = addrs.length;
		final String name = Integer.toString(instanceN++) + '-' +
			StringUtils.capitalize(reqConf.getAPI().toLowerCase()) + '-' +
			StringUtils.capitalize(reqConf.getLoadType().toString().toLowerCase()) +
			(maxCount>0? Long.toString(maxCount) : "") + '-' +
			Integer.toString(threadsPerNode) + 'x' + Integer.toString(nodeCount);
		setName(name);
		this.threadsPerNode = threadsPerNode;
		this.maxCount = maxCount>0? maxCount : Long.MAX_VALUE;
		// init metrics
		counterSubm = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUBM));
		counterReqSucc = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUCC));
		counterReqFail = metrics.counter(MetricRegistry.name(name, METRIC_NAME_FAIL));
		reqBytes = metrics.meter(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_BW));
		reqDur = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_DUR));
		metricsReporter.start();
		// prepare the node executors array
		nodes = new WSNodeExecutor[nodeCount];
		// create and configure the connection manager for HTTP client
		final int totalThreadCount = threadsPerNode * nodeCount;
		connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setDefaultMaxPerRoute(threadsPerNode);
		connMgr.setMaxTotal(totalThreadCount);
		connMgr.setDefaultConnectionConfig(
			ConnectionConfig
				.custom()
				.build()
		);
		// set shared headers to client builder
		final LinkedList<Header> headers = new LinkedList<>();
		final Map<String, String> sharedHeadersMap = reqConf.getSharedHeadersMap();
		for(final String key: sharedHeadersMap.keySet()) {
			headers.add(new BasicHeader(key, sharedHeadersMap.get(key)));
		}
		// configure and create the HTTP client
		httpClient = HttpClientBuilder
			.create()
			.setConnectionManager(connMgr)
			.setDefaultHeaders(headers)
			.setRetryHandler(reqConf.retryHandler)
			.disableCookieManagement()
			//.disableAutomaticRetries()
			.setUserAgent(WSRequestConfig.DEFAULT_USERAGENT)
			.setMaxConnPerRoute(threadsPerNode)
			.setMaxConnTotal(totalThreadCount)
			.build();
		//
		reqConf.setClient(httpClient);
		dataSrc = reqConf.getDataSource();
		// if path specified use the file as producer
		if(listFile!=null && listFile.length()>0) {
			try {
				producer = new FileProducer<>(listFile, WSObject.class);
				producer.setConsumer(this);
			} catch(final NoSuchMethodException e) {
				LOG.fatal(Markers.ERR, "Failed to get the constructor", e);
			} catch(final IOException e) {
				LOG.warn(Markers.ERR, "Failed to use object list file \"{}\"for reading", listFile);
				LOG.debug(Markers.ERR, e.toString(), e.getCause());
			}
		}
		//
		int submitThreadCount = threadsPerNode * addrs.length;
		submitExecutor = new ThreadPoolExecutor(
			submitThreadCount, submitThreadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(submitThreadCount*REQ_QUEUE_FACTOR),
			new RejectedTaskHandler()
		);
		WSNodeExecutor nodeExecutor;
		for(short i=0; i<addrs.length; i++) {
			nodeExecutor = new WSNodeExecutor(
				addrs[i], threadsPerNode, reqConf, metrics, getName()
			);
			nodes[i] = nodeExecutor;
		}
		// by default, may be overriden later externally
		setConsumer(new LogConsumer<WSObject>());
	}
	//
	@Override
	public final void start() {
		//
		if(producer==null) {
			LOG.info(Markers.MSG, "Waiting for incoming objects to process from external producer");
		} else {
			try {
				producer.start();
				LOG.debug(
					Markers.MSG, "Started object producer {}",
					producer.getClass().getSimpleName()
				);
			} catch(final IOException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to stop the producer");
			}
		}
		//
		super.start();
		LOG.debug(Markers.MSG, "Started {}", getName());
	}
	//
	@Override
	public final void run() {
		//
		try {
			do {
				logMetrics(Markers.PERF_AVG);
				TimeUnit.SECONDS.sleep(METRICS_UPDATE_PERIOD_SEC); // sleep
			} while(maxCount > counterReqSucc.getCount() + counterReqFail.getCount());
		} catch(final InterruptedException e) {
			LOG.trace(Markers.MSG, "Externally interrupted \"{}\"", getName());
		}
		//
		interrupt();
		//
	}
	//
	@Override
	public final synchronized void interrupt() {
		//
		maxCount = counterSubm.getCount();
		//
		if(producer!=null) {
			try {
				producer.interrupt();
				LOG.debug(Markers.MSG, "Stopped object producer {}", producer.toString());
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.WARN, e,
					String.format("Failed to stop the producer: %s", producer.toString())
				);
			}
		}
		//
		submitExecutor.shutdown();
		submitExecutor.getQueue().clear();
		try {
			submitExecutor.awaitTermination(1, TimeUnit.MINUTES);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "Interrupted while awaiting the submitter termination");
		}
		//
		final Thread interrupters[] = new Thread[nodes.length];
		for(int i=0; i<nodes.length; i++) {
			final WSNodeExecutor nextNode = nodes[i];
			interrupters[i] = new Thread() {
				@Override
				public final void run() {
					nextNode.interrupt();
				}
			};
			interrupters[i].start();

		}
		//
		for(int i=0; i<nodes.length; i++) {
			try {
				interrupters[i].join();
				LOG.debug(Markers.MSG, "Interrupted node executor {}", nodes[i]);
			} catch(final InterruptedException e) {
				LOG.debug(Markers.ERR, e.toString());
			}
		}
		//
		super.interrupt();
		LOG.debug(Markers.MSG, "Interrupted \"{}\"", getName());
	}
	//
	@Override
	public synchronized void close()
	throws IOException {
		//
		if(!isInterrupted()) {
			interrupt();
		}
		//
		LOG.debug(Markers.MSG, "Dropped {} tasks on closing", submitExecutor.shutdownNow().size());
		for(final WSNodeExecutor nodeExecutor: nodes) {
			try {
				nodeExecutor.close();
				LOG.debug(Markers.MSG, "Closed the node executor {}", nodeExecutor);
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.WARN, e,
					String.format("Failed to stop the node executor: %s", nodeExecutor.getName())
				);
			}
		}
		//
		try {
			httpClient.close();
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to close the HTTP client");
		}
		//
		synchronized(LOG) {
			LOG.info(Markers.PERF_SUM, "Summary metrics below for {}", getName());
			logMetrics(Markers.PERF_SUM);
		}
		metricsReporter.close();
		//
		LOG.debug(Markers.MSG, "Closed {}", getName());
	}
	//
	@Override
	public void submit(final WSObject object) {
		if(object==null) { // handle the poison
			maxCount = counterSubm.getCount();
			for(final WSNodeExecutor nextNode : nodes) {
				if(!nextNode.isShutdown()) {
					nextNode.submit(WSObject.class.cast(null));
				}
			}
		} else if(!isInterrupted()) {
			submitExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						nodes[(int) counterSubm.getCount() % nodes.length].submit(object);
					}
				}
			);
		}
	}
	//
	private void logMetrics(final Marker logMarker) {
		//
		final long
			countReqSucc = counterReqSucc.getCount(),
			countBytes = reqBytes.getCount();
		final double
			avgSize = countReqSucc==0 ? 0 : (double) countBytes / countReqSucc,
			meanBW = reqBytes.getMeanRate(),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		final Snapshot reqDurSnapshot = reqDur.getSnapshot();
		//
		int notCompletedTaskCount = 0;
		for(final WSNodeExecutor nodeExecutor: nodes) {
			notCompletedTaskCount += nodeExecutor.getQueue().size() + nodeExecutor.getActiveCount();
		}
		notCompletedTaskCount += submitExecutor.getQueue().size() + submitExecutor.getActiveCount();
		//
		LOG.info(
			logMarker,
			MSG_FMT_METRICS.format(
				new Object[] {
					countReqSucc, notCompletedTaskCount, counterReqFail.getCount(),
					//
					(float)reqDurSnapshot.getMin() / BILLION,
					(float)reqDurSnapshot.getMedian() / BILLION,
					(float)reqDurSnapshot.getMean() / BILLION,
					(float)reqDurSnapshot.getMax() / BILLION,
					//
					avgSize==0 ? 0 : meanBW/avgSize,
					avgSize==0 ? 0 : oneMinBW/avgSize,
					avgSize==0 ? 0 : fiveMinBW/avgSize,
					avgSize==0 ? 0 : fifteenMinBW/avgSize,
					//
					meanBW/MIB, oneMinBW/MIB, fiveMinBW/MIB, fifteenMinBW/MIB
				}
			)
		);
		//
		if(LOG.isDebugEnabled(Markers.PERF_AVG)) {
			for(final WSNodeExecutor node: nodes) {
				node.logMetrics(Level.DEBUG, Markers.PERF_AVG);
			}
		}
		//
		LOG.trace(Markers.PERF_AVG, "Connection pool: {}", connMgr.getTotalStats());
		//
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public final void setMaxCount(final long maxCount) {
		this.maxCount = maxCount;
	}
	//
	public final int getThreadCount() {
		return threadsPerNode * nodes.length;
	}
	//
	@Override
	public final Consumer<WSObject> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void setConsumer(final Consumer<WSObject> consumer) {
		this.consumer = consumer;
		for(final WSNodeExecutor node: nodes) {
			node.setConsumer(consumer);
		}
		LOG.debug(
			Markers.MSG, "Appended the consumer \"{}\" for producer \"{}\"", consumer, getName()
		);
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	//
	@Override
	public final Producer<WSObject> getProducer() {
		return producer;
	}
	//
	public final UniformDataSource getDataSource() {
		return dataSrc;
	}
}
