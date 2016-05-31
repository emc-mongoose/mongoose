package com.emc.mongoose.core.impl.load.model.metrics;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 14.09.15.
 */
public class BasicIoStats
extends IoStatsBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final int updateIntervalSec;
	protected final AtomicLong reqDurationSum = new AtomicLong(0);
	protected final AtomicLong respLatencySum = new AtomicLong(0);
	//
	protected CustomMeter throughPutSucc, throughPutFail, reqBytes;
	//
	public BasicIoStats(
		final String name, final boolean serveJmxFlag, final int updateIntervalSec
	) {
		super(name, serveJmxFlag);
		this.updateIntervalSec = updateIntervalSec;
	}
	//
	@Override
	public void start() {
		// init load exec time dependent metrics
		throughPutSucc = metrics.register(
			CustomMetricRegistry.name(name, METRIC_NAME_SUCC),
			new CustomMeter(clock, updateIntervalSec)
		);
		throughPutFail = metrics.register(
			CustomMetricRegistry.name(name, METRIC_NAME_FAIL),
			new CustomMeter(clock, updateIntervalSec)
		);
		reqBytes = metrics.register(
			CustomMetricRegistry.name(name, METRIC_NAME_BYTE),
			new CustomMeter(clock, updateIntervalSec)
		);
		//
		super.start();
	}
	//
	@Override
	public void markSucc(final long size, final int duration, final int latency) {
		throughPutSucc.mark();
		reqBytes.mark(size);
		reqDuration.update(duration);
		reqDurationSum.addAndGet(duration);
		respLatencySum.addAndGet(latency);
		respLatency.update(latency);
	}
	//
	@Override
	public void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	) {
		throughPutSucc.mark(count);
		reqBytes.mark(bytes);
		for(final long duration : durationValues) {
			reqDuration.update(duration);
			reqDurationSum.addAndGet(duration);
		}
		for(final long latency : latencyValues) {
			respLatency.update(latency);
			respLatencySum.addAndGet(latency);
		}
	}
	//
	@Override
	public void markFail() {
		throughPutFail.mark();
	}
	//
	@Override
	public void markFail(final long count) {
		throughPutFail.mark(count);
	}
	//
	@Override
	public Snapshot getSnapshot() {
		final long currElapsedTime = tsStartMicroSec > 0 ?
			TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) - tsStartMicroSec : 0;
		final com.codahale.metrics.Snapshot reqDurSnapshot = reqDuration.getSnapshot();
		LockSupport.parkNanos(1_000);
		final com.codahale.metrics.Snapshot respLatSnapshot = respLatency.getSnapshot();
		LockSupport.parkNanos(1_000);
		return new BasicSnapshot(
			throughPutSucc == null ? 0 : throughPutSucc.getCount(),
			throughPutSucc == null ? 0 : throughPutSucc.getLastRate(),
			throughPutFail == null ? 0 : throughPutFail.getCount(),
			throughPutFail == null ? 0 : throughPutFail.getLastRate(),
			reqBytes == null ? 0 : reqBytes.getCount(),
			reqBytes == null ? 0 : reqBytes.getLastRate(),
			prevElapsedTimeMicroSec + currElapsedTime, reqDurationSum.get(), respLatencySum.get(),
			reqDurSnapshot, respLatSnapshot
		);
	}
}
