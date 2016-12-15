package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.IoBuffer;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.load.monitor.metrics.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 15.12.16.
 */
public final class PostProcessIoResultsSvcTask<R extends IoTask.IoResult>
implements Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private final LoadMonitor monitor;
	private final IoBuffer<R> ioTaskResultsQueue;
	private final boolean ioTraceOutputFlag;
	private final Output<String> itemInfoOutput;
	private final Map<String, String> uniqueItems;
	private final Int2ObjectMap<IoStats> ioStats;
	private final Int2ObjectMap<IoStats> medIoStats;
	private final LongAdder counterResults;

	public PostProcessIoResultsSvcTask(
		final LoadMonitor monitor, final IoBuffer<R> ioTaskResultsQueue,
		final boolean ioTraceOutputFlag, final Output<String> itemInfoOutput,
		final Map<String, String> uniqueItems, final Int2ObjectMap<IoStats> ioStats,
		final Int2ObjectMap<IoStats> medIoStats, final LongAdder counterResults
	) {
		this.monitor = monitor;
		this.ioTaskResultsQueue = ioTaskResultsQueue;
		this.ioTraceOutputFlag = ioTraceOutputFlag;
		this.itemInfoOutput = itemInfoOutput;
		this.uniqueItems = uniqueItems;
		this.ioStats = ioStats;
		this.medIoStats = medIoStats;
		this.counterResults = counterResults;
	}

	@Override
	public final void run() {
		final List<R> ioResults = new ArrayList<>(BATCH_SIZE);
		int ioResultsCount;
		final Thread currThread = Thread.currentThread();
		currThread.setName(currThread.getName() + "-" + "postProcessIoResults");
		while(!currThread.isInterrupted()) {
			ioResults.clear();
			try {
				ioResultsCount = ioTaskResultsQueue.get(ioResults, BATCH_SIZE);
				if(ioResultsCount > 0) {
					postProcessIoResults(ioResults, ioResultsCount);
				} else {
					if(monitor.isInterrupted() || monitor.isClosed()) {
						break;
					} else {
						try {
							Thread.sleep(1);
						} catch(final InterruptedException ignored) {
						}
					}
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to postprocess the I/O results");
			}
		}
	}

	private void postProcessIoResults(final List<R> ioTaskResults, final int n) {

		int m = n; // count of complete whole tasks
		int itemInfoCommaPos;

		// I/O trace logging
		if(ioTraceOutputFlag) {
			LOG.debug(Markers.IO_TRACE, new IoTraceCsvBatchLogMessage<>(ioTaskResults, 0, n));
		}

		R ioTaskResult;
		DataIoTask.DataIoResult dataIoTaskResult;
		int ioTypeCode;
		int statusCode;
		String itemInfo;
		long reqDuration;
		long respLatency;
		long countBytesDone = 0;
		ioTaskResult = ioTaskResults.get(0);
		final boolean isDataTransferred = ioTaskResult instanceof DataIoTask.DataIoResult;
		IoStats ioTypeStats, ioTypeMedStats;

		final List<String> itemsToPass = itemInfoOutput == null ? null : new ArrayList<>(n);

		for(int i = 0; i < n; i ++) {

			if(i > 0) {
				ioTaskResult = ioTaskResults.get(i);
			}

			if( // account only completed composite I/O tasks
				ioTaskResult instanceof CompositeIoTask.CompositeIoResult &&
					!((CompositeIoTask.CompositeIoResult) ioTaskResult).getCompleteFlag()
				) {
				m --;
				continue;
			}

			ioTypeCode = ioTaskResult.getIoTypeCode();
			statusCode = ioTaskResult.getStatusCode();
			reqDuration = ioTaskResult.getDuration();
			respLatency = ioTaskResult.getLatency();
			if(isDataTransferred) {
				dataIoTaskResult = (DataIoTask.DataIoResult) ioTaskResult;
				countBytesDone = dataIoTaskResult.getCountBytesDone();
			}

			ioTypeStats = ioStats.get(ioTypeCode);
			ioTypeMedStats = medIoStats.get(ioTypeCode);

			if(statusCode == IoTask.Status.SUCC.ordinal()) {
				if(respLatency > 0 && respLatency > reqDuration) {
					LOG.debug(Markers.ERR, "Dropping invalid latency value {}", respLatency);
				}
				if(ioTaskResult instanceof PartialIoTask.PartialIoResult) {
					ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markPartSucc(countBytesDone, reqDuration, respLatency);
					}
					m --;
				} else {
					itemInfo = ioTaskResult.getItemInfo();
					if(uniqueItems != null) {
						itemInfoCommaPos = itemInfo.indexOf(',', 0);
						if(itemInfoCommaPos > 0) {
							uniqueItems.put(itemInfo.substring(0, itemInfoCommaPos), itemInfo);
						} else {
							uniqueItems.put(itemInfo, itemInfo);
						}
					} else if(itemInfoOutput != null) {
						itemsToPass.add(itemInfo);
					}
					// update the metrics with success
					ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
					}
				}

			} else {
				ioTypeStats.markFail();
				if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
					ioTypeMedStats.markFail();
				}
			}
		}

		if(uniqueItems == null && itemInfoOutput != null) {
			final int itemsToPassCount = itemsToPass.size();
			try {
				for(
					int i = 0; i < itemsToPassCount;
					i += itemInfoOutput.put(itemsToPass, i, itemsToPassCount)
				);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to output {} items to {}", itemsToPassCount,
					itemInfoOutput
				);
			}
		}

		counterResults.add(m);
	}
}