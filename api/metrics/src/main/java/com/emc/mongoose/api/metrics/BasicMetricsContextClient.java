package com.emc.mongoose.api.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.UniformReservoir;
import com.emc.mongoose.api.model.io.IoType;
import com.github.akurilov.commons.system.SizeInBytes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BasicMetricsContextClient
implements MetricsContext {

	private final String stepId;
	private final IoType ioType;
	private final int concurrency;
	private final int thresholdConcurrency;
	private final SizeInBytes itemDataSize;
	private final boolean stdOutColorFlag;
	private final boolean avgPersistFlag;
	private final boolean sumPersistFlag;
	private final boolean perfDbResultsFileFlag;
	private final long outputPeriodMillis;
	private volatile long lastOutputTs = 0;
	private volatile MetricsSnapshot lastSnapshot = null;
	private volatile MetricsListener metricsListener = null;
	private volatile MetricsContext thresholdMetricsCtx = null;
	private volatile boolean thresholdStateExitedFlag = false;

	public BasicMetricsContextClient(
		final String stepId, final IoType ioType, final int concurrency,
		final int thresholdConcurrency, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag, final boolean avgPersistFlag, final boolean sumPersistFlag,
		final boolean perfDbResultsFileFlag
	) {
		this.stepId = stepId;
		this.ioType = ioType;
		this.concurrency = concurrency;
		this.thresholdConcurrency = thresholdConcurrency > 0 ?
			thresholdConcurrency : Integer.MAX_VALUE;
		this.itemDataSize = itemDataSize;

		this.stdOutColorFlag = stdOutColorFlag;
		this.avgPersistFlag = avgPersistFlag;
		this.sumPersistFlag = sumPersistFlag;
		this.perfDbResultsFileFlag = perfDbResultsFileFlag;
		this.outputPeriodMillis = TimeUnit.SECONDS.toMillis(updateIntervalSec);
	}

	@Override
	public void start() {

	}

	@Override
	public boolean isStarted() {
		return false;
	}

	@Override
	public void markSucc(final long size, final long duration, final long latency) {

	}

	@Override
	public void markPartSucc(final long size, final long duration, final long latency) {

	}

	@Override
	public void markSucc(
		final long count, final long bytes, final long[] durationValues, final long[] latencyValues
	) {

	}

	@Override
	public void markPartSucc(
		final long bytes, final long[] durationValues, final long[] latencyValues
	) {

	}

	@Override
	public void markFail() {

	}

	@Override
	public void markFail(final long count) {

	}

	@Override
	public void markElapsedTime(final long millis) {

	}

	@Override
	public String getStepId() {
		return null;
	}

	@Override
	public IoType getIoType() {
		return null;
	}

	@Override
	public int getNodeCount() {
		return 0;
	}

	@Override
	public int getConcurrency() {
		return 0;
	}

	@Override
	public int getConcurrencyThreshold() {
		return 0;
	}

	@Override
	public int getActualConcurrency() {
		return 0;
	}

	@Override
	public SizeInBytes getItemDataSize() {
		return null;
	}

	@Override
	public boolean getStdOutColorFlag() {
		return false;
	}

	@Override
	public boolean getAvgPersistFlag() {
		return false;
	}

	@Override
	public boolean getSumPersistFlag() {
		return false;
	}

	@Override
	public boolean getPerfDbResultsFileFlag() {
		return false;
	}

	@Override
	public long getOutputPeriodMillis() {
		return 0;
	}

	@Override
	public long getLastOutputTs() {
		return 0;
	}

	@Override
	public void setLastOutputTs(final long ts) {

	}

	@Override
	public void refreshLastSnapshot() {

	}

	@Override
	public MetricsSnapshot getLastSnapshot() {
		return null;
	}

	@Override
	public void setMetricsListener(final MetricsListener metricsListener) {

	}

	@Override
	public boolean isThresholdStateEntered() {
		return false;
	}

	@Override
	public void enterThresholdState()
	throws IllegalStateException {

	}

	@Override
	public boolean isThresholdStateExited() {
		return false;
	}

	@Override
	public MetricsContext getThresholdMetrics() {
		return null;
	}

	@Override
	public void exitThresholdState()
	throws IllegalStateException {

	}

	@Override
	public void close()
	throws IOException {

	}
}
