package com.emc.mongoose.base.load.step.client;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.emc.mongoose.base.load.step.client.LoadStepClient.OUTPUT_PROGRESS_PERIOD_MILLIS;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import com.emc.mongoose.base.load.step.file.FileManager;
import com.emc.mongoose.base.load.step.service.file.FileManagerService;
import com.emc.mongoose.base.logging.LogContextThreadFactory;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.commons.system.SizeInBytes;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;

public class OpTraceLogFileAggregator implements Closeable {

	private final String loadStepId;
	private final Map<FileManager, String> opTraceLogFileSlices;

	public OpTraceLogFileAggregator(final String loadStepId, final List<FileManager> fileMgrs) {
		this.loadStepId = loadStepId;
		this.opTraceLogFileSlices = fileMgrs.stream()
						// exclude local I/O trace log file
						.filter(fileMgr -> fileMgr instanceof FileManagerService)
						.collect(
										Collectors.toMap(
														Function.identity(),
														fileMgr -> {
															String ioTraceLogFileSliceName = null;
															try {
																ioTraceLogFileSliceName = fileMgr.logFileName(Loggers.OP_TRACES.getName(), loadStepId);
																Loggers.MSG.debug(
																				"{}: the remote file manager \"{}\" returned the file name \"{}\" for the I/O traces",
																				loadStepId,
																				fileMgr,
																				ioTraceLogFileSliceName);
															} catch (final IOException e) {
																LogUtil.exception(
																				Level.WARN,
																				e,
																				"{}: failed to get the remote log file name",
																				loadStepId);
															}
															return ioTraceLogFileSliceName;
														}));
	}

	public final void collectToLocal() {

		final LongAdder byteCounter = new LongAdder();
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
						2, new LogContextThreadFactory("collectOpTraceLogFileWorker", true));
		final CountDownLatch finishLatch = new CountDownLatch(1);

		executor.submit(
						() -> {
							try {
								opTraceLogFileSlices
												.entrySet()
												.parallelStream()
												// don't transfer the local file data
												.filter(entry -> entry.getKey() instanceof FileManagerService)
												.forEach(
																entry -> {
																	final var fileMgr = entry.getKey();
																	final var remoteIoTraceLogFileName = entry.getValue();
																	transferToLocal(fileMgr, remoteIoTraceLogFileName, byteCounter);
																	try {
																		fileMgr.deleteFile(remoteIoTraceLogFileName);
																	} catch (final Exception e) {
																		throwUncheckedIfInterrupted(e);
																		LogUtil.exception(
																						Level.WARN,
																						e,
																						"{}: failed to delete the file \"{}\" @ file manager \"{}\"",
																						loadStepId,
																						remoteIoTraceLogFileName,
																						fileMgr);
																	}
																});
							} finally {
								finishLatch.countDown();
							}
						});
		executor.scheduleAtFixedRate(
						() -> Loggers.MSG.info(
										"\"{}\": transferred {} I/O trace data...",
										loadStepId,
										SizeInBytes.formatFixedSize(byteCounter.longValue())),
						0,
						OUTPUT_PROGRESS_PERIOD_MILLIS,
						TimeUnit.MILLISECONDS);

		try {
			finishLatch.await();
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} finally {
			executor.shutdownNow();
			Loggers.MSG.info(
							"\"{}\": transferred {} of the operation traces data",
							loadStepId,
							SizeInBytes.formatFixedSize(byteCounter.longValue()));
		}
	}

	private static void transferToLocal(
					final FileManager fileMgr,
					final String remoteIoTraceLogFileName,
					final LongAdder byteCounter) {
		long transferredByteCount = 0;
		try (final Instance logCtx = put(KEY_CLASS_NAME, OpTraceLogFileAggregator.class.getSimpleName())) {
			byte[] data;
			while (true) {
				data = fileMgr.readFromFile(remoteIoTraceLogFileName, transferredByteCount);
				Loggers.OP_TRACES.info(new String(data));
				transferredByteCount += data.length;
				byteCounter.add(data.length);
			}
		} catch (final EOFException ok) {} catch (final RemoteException e) {
			LogUtil.exception(Level.WARN, e, "Failed to read the data from the remote file");
		} catch (final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected I/O exception");
		} finally {
			Loggers.MSG.debug(
							"Transferred {} of the remote operation traces data from the remote file \"{}\" @ \"{}\"",
							SizeInBytes.formatFixedSize(transferredByteCount),
							remoteIoTraceLogFileName,
							fileMgr);
		}
	}

	@Override
	public final void close() {
		try (final Instance logCtx = put(KEY_STEP_ID, loadStepId).put(KEY_CLASS_NAME, getClass().getSimpleName())) {
			collectToLocal();
		}
		opTraceLogFileSlices.clear();
	}
}
