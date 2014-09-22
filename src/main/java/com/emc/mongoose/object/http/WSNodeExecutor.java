package com.emc.mongoose.object.http;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.Consumer;
import com.emc.mongoose.LoadExecutor;
import com.emc.mongoose.Producer;
import com.emc.mongoose.api.RequestConfig;
import com.emc.mongoose.logging.ExceptionHandler;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.api.WSRequest;
import com.emc.mongoose.object.http.api.WSRequestConfig;
import com.emc.mongoose.threading.WorkerFactory;
//
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import java.io.IOException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 06.06.14.
 */
public class WSNodeExecutor
extends ThreadPoolExecutor
implements LoadExecutor<WSObject> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final WSRequestConfig localReqConf;
	private final Counter counterSubm, counterRej, counterReqSucc, counterReqFail;
	private final Counter counterSubmParent, counterRejParent, counterReqSuccParent, counterReqFailParent;
	private final Meter reqBytes, reqBytesParent;
	private final Histogram reqDur, reqDurParent;
	//
	private volatile Consumer<WSObject> consumer = null;
	//
	private WSNodeExecutor(
		final int threadsPerNode, final WSRequestConfig localReqConf,
		final MetricRegistry parentMetrics, final String parentName
	) {
		super(
			threadsPerNode, threadsPerNode, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(threadsPerNode * REQ_QUEUE_FACTOR),
			new WorkerFactory(parentName+'<'+localReqConf.getAddr()+'>')
		);
		this.localReqConf = localReqConf;
		//
		counterSubm = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_SUBM));
		counterSubmParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_SUBM)
		);
		//
		counterRej = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_REJ));
		counterRejParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_REJ)
		);
		//
		counterReqSucc = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_SUCC));
		counterReqSuccParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_SUCC)
		);
		//
		counterReqFail = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_FAIL));
		counterReqFailParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_FAIL)
		);
		//
		reqDur = parentMetrics.histogram(
			MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_DUR)
		);
		reqDurParent = parentMetrics.getHistograms().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_DUR)
		);
		//
		reqBytes = parentMetrics.meter(
			MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_BW)
		);
		reqBytesParent = parentMetrics.getMeters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_BW)
		);
		//
	}
	//
	public WSNodeExecutor(
		final String addr, final int threadsPerNode, final WSRequestConfig sharedReqConf,
		final MetricRegistry parentMetrics, final String parentName
	) {
		this(threadsPerNode, sharedReqConf.clone().setAddr(addr), parentMetrics, parentName);
	}
	//
	@Override
	public final void setConsumer(final Consumer<WSObject> consumer) {
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<WSObject> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void submit(final WSObject object) {
		final WSRequest request = WSRequest.getInstanceFor(localReqConf, object);
		boolean passed = false;
		int rejectCount = 0;
		while(!passed && rejectCount < LoadExecutor.COUNT_RETRY_MAX) {
			try {
				super.submit(request);
				passed = true;
			} catch(final RejectedExecutionException e) {
				rejectCount ++;
				try {
					Thread.sleep(rejectCount * LoadExecutor.WAIT_QUANT_MILLISEC);
				} catch(final InterruptedException ee) {
					break;
				}
			}
		}
		//
		if(object!=null) {
			counterSubm.inc();
			counterSubmParent.inc();
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Request for the object {} succesfully submitted",
					Long.toHexString(object.getId())
				);
			}
		}
	}
	//
	@Override
	protected final void afterExecute(final Runnable reqTask, final Throwable thrown) {
		if(thrown!=null) {
			LOG.warn(Markers.ERR, thrown.toString());
			counterReqFail.inc(); counterReqFailParent.inc();
		} else {
			try(final WSRequest request = WSRequest.class.cast(Future.class.cast(reqTask).get())) {
				final WSObject object = request.getDataItem();
				final int statusCode = request.getStatusCode();
				//
				if(statusCode < 300) {
					// update the metrics with success
					counterReqSucc.inc();
					counterReqSuccParent.inc();
					final long
						duration = request.getDuration(),
						size = object.getSize();
					reqBytes.mark(size);
					reqBytesParent.mark(size);
					reqDur.update(duration);
					reqDurParent.update(duration);
					// feed to the consumer
					if(consumer!=null) {
						try {
							consumer.submit(object);
						} catch(final IOException e) {
							ExceptionHandler.trace(
								LOG, Level.WARN, e,
								String.format("Failed to submit the object \"%s\" to consumer", object)
							);
						}
					}
				} else {
					counterReqFail.inc();
					counterReqFailParent.inc();
				}
				//
			} catch(final InterruptedException e) {
				counterReqFail.inc(); counterReqFailParent.inc();
				LOG.trace(Markers.ERR, "Interrupted while waiting for the response");
			} catch(final CancellationException e) {
				counterReqFail.inc(); counterReqFailParent.inc();
				LOG.warn(Markers.ERR, "Request has been cancelled:", e);
				counterReqFail.inc(); counterReqFailParent.inc();
			} catch(final ExecutionException e) {
				if(isShutdown()) {
					LOG.trace(Markers.ERR, "Request interrupted due to node executor shutdown");
					counterRej.inc(); counterRejParent.inc();
				} else {
					final Throwable cause = e.getCause();
					if(InterruptedException.class.isInstance(cause)) {
						LOG.trace(Markers.MSG, "Poisoned");
						/*try {
							consumer.submit(null); // pass the poison through the consumer-producer chain
						} catch(final RemoteException ee) {
							LOG.debug(Markers.ERR, "Failed to feed the poison to consumer due to {}", ee.toString());
						}*/
					} else {
						counterReqFail.inc();
						counterReqFailParent.inc();
						if(NoHttpResponseException.class.isInstance(cause)) {
							LOG.warn(
								Markers.ERR, "No HTTP response from the server {}",
								localReqConf.getAddr()
							);
						} else if(ConnectionPoolTimeoutException.class.isInstance(cause)) {
							LOG.warn(
								Markers.ERR, "Timeout while waiting for available connection from pool"
							);
						} else if(ConnectTimeoutException.class.isInstance(cause)) {
							LOG.warn(Markers.ERR, "Connection timeout");
							////////////////////////////////////////////////////////////////////////
						} else if(SocketTimeoutException.class.isInstance(cause)) {
							LOG.warn(Markers.ERR, "Request timeout");
						} else if(PortUnreachableException.class.isInstance(cause)) {
							LOG.warn(
								Markers.ERR,
								"Service unreachable. Check that port number ({}) is correct.",
								localReqConf.getPort()
							);
						} else if(ConnectException.class.isInstance(cause)) {
							LOG.warn(
								Markers.ERR, "Failed to connect to the server {}",
								localReqConf.getAddr()
							);
						} else if(SocketException.class.isInstance(cause)) {
							LOG.warn(Markers.ERR, "Network failure: {}", e.toString());
							////////////////////////////////////////////////////////////////////////
						} else if(HttpResponseException.class.isInstance(cause)) {
							LOG.warn(Markers.ERR, "HTTP response marked as failed");
						} else {
							LOG.error(Markers.ERR, "Request execution failure");
							if(LOG.isTraceEnabled(Markers.ERR) && cause!=null) {
								LOG.trace(Markers.ERR, e.toString(), cause);
							}
						}
					}
				}
			} catch(final Exception e) {
				counterReqFail.inc(); counterReqFailParent.inc();
				LOG.warn(Markers.MSG, reqTask.getClass().getCanonicalName());
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Unexpected failure");
			}
		}
		//
		super.afterExecute(reqTask, thrown);
	}
	//
	@Override
	public final String toString() {
		return getThreadFactory().toString();
	}
	//
	public final void logMetrics(final Level logLevel, final Marker logMarker) {
		final Snapshot reqDurSnapshot = reqDur.getSnapshot();
		final long
			countReqSucc = counterReqSucc.getCount(),
			countBytes = reqBytes.getCount();
		final double
			avgSize = countReqSucc==0 ? 0 : (double) countBytes / countReqSucc,
			meanBW = reqBytes.getMeanRate(),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		LOG.log(
			logLevel, logMarker,
			localReqConf.getAddr() + ": " + LoadExecutor.MSG_FMT_METRICS.format(
				new Object[] {
					countReqSucc, getQueue().size() + getActiveCount(), counterReqFail.getCount(),
					//
					(float) reqDurSnapshot.getMin() / LoadExecutor.BILLION,
					(float) reqDurSnapshot.getMedian() / LoadExecutor.BILLION,
					(float) reqDurSnapshot.getMean() / LoadExecutor.BILLION,
					(float) reqDurSnapshot.getMax() / LoadExecutor.BILLION,
					//
					avgSize==0 ? 0 : meanBW/avgSize,
					avgSize==0 ? 0 : oneMinBW/avgSize,
					avgSize==0 ? 0 : fiveMinBW/avgSize,
					avgSize==0 ? 0 : fifteenMinBW/avgSize,
					//
					meanBW/LoadExecutor.MIB, oneMinBW/LoadExecutor.MIB,
					fiveMinBW/LoadExecutor.MIB, fifteenMinBW/LoadExecutor.MIB
				}
			)
		);
		if(LOG.isTraceEnabled(Markers.PERF_AVG)) {
			LOG.trace(
				Markers.PERF_AVG,
				"{} internal metrics: shutdown: {}, terminated: {}, tasks: {} running, {} done, {} waiting",
				toString(), isShutdown(), isTerminated(), getActiveCount(), getCompletedTaskCount(),
				getQueue().size()
			);
		}
	}
	//
	@Override
	public final String getName() {
		return getThreadFactory().toString();
	}
	//
	@Override
	public final Producer<WSObject> getProducer() {
		return null;
	}
	//
	@Override
	public final long getMaxCount()
	throws RemoteException {
		return consumer.getMaxCount();
	}
	//
	@Override
	public final void setMaxCount(final long maxCount) {
	}
	//
	public final String getAddr() {
		return localReqConf.getAddr();
	}
	//
	@Override
	public final void start() {
		prestartAllCoreThreads();
	}
	//
	@Override
	public final void interrupt() {
		LOG.debug(Markers.MSG, "Interrupting...");
		localReqConf.setRetries(false);
		shutdown();
		getQueue().clear();
		try {
			LOG.debug(
				Markers.MSG, "Wait at most {} ms before terminating {}+{} tasks",
				RequestConfig.REQUEST_TIMEOUT_MILLISEC, getQueue().size(), getActiveCount()
			);
			awaitTermination(RequestConfig.REQUEST_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "Interrupted while waiting the submitted tasks to finish");
		}
	}
	//
	@Override
	public final void close()
	throws IOException {
		if(!isShutdown()) {
			interrupt();
		}
		LOG.debug(Markers.MSG, "Dropping {} tasks", shutdownNow().size());
		synchronized(LOG) {
			LOG.debug(Markers.PERF_SUM, "Summary metrics below for {}", getName());
			logMetrics(Level.DEBUG, Markers.PERF_SUM);
		}
		//
		LOG.debug(Markers.MSG, "Closed {}", getThreadFactory().toString());
	}
	//
}
