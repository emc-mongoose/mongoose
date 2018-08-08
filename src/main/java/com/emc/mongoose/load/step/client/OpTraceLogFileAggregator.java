package com.emc.mongoose.load.step.client;

import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.load.step.client.LoadStepClient.OUTPUT_PROGRESS_PERIOD_MILLIS;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.load.step.file.FileManager;
import com.emc.mongoose.load.step.service.file.FileManagerService;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.system.SizeInBytes;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.Level;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OpTraceLogFileAggregator
implements Closeable {

	private final String loadStepId;
	private final Map<FileManager, String> opTraceLogFileSlices;

	public OpTraceLogFileAggregator(final String loadStepId, final List<FileManager> fileMgrs) {
		this.loadStepId = loadStepId;
		this.opTraceLogFileSlices = fileMgrs
			.stream()
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
								loadStepId, fileMgr, ioTraceLogFileSliceName
							);
						} catch(final IOException e) {
							LogUtil.exception(Level.WARN, e, "{}: failed to get the remote log file name", loadStepId);
						}
						return ioTraceLogFileSliceName;
					}
				)
			);
	}

	public final void collectToLocal() {

		final LongAdder byteCounter = new LongAdder();
		final Thread progressOutputThread = new Thread(
			() -> {
				Loggers.MSG.info("\"{}\": start to transfer the I/O trace data to the local log file", loadStepId);
				try {
					while(true) {
						TimeUnit.MILLISECONDS.sleep(OUTPUT_PROGRESS_PERIOD_MILLIS);
						Loggers.MSG.info(
							"\"{}\": transferred {} I/O trace data...", loadStepId,
							SizeInBytes.formatFixedSize(byteCounter.longValue())
						);
					}
				} catch(final InterruptedException e) {
					throw new InterruptRunException(e);
				}
			}
		);
		progressOutputThread.setDaemon(true);
		progressOutputThread.start();

		try {
			opTraceLogFileSlices
				.entrySet()
				.parallelStream()
				// don't transfer the local file data
				.filter(entry -> entry.getKey() instanceof FileManagerService)
				.forEach(
					entry -> {
						final FileManager fileMgr = entry.getKey();
						final String remoteIoTraceLogFileName = entry.getValue();
						transferToLocal(fileMgr, remoteIoTraceLogFileName, byteCounter);
						try {
							fileMgr.deleteFile(remoteIoTraceLogFileName);
						} catch(final Exception e) {
							LogUtil.exception(
								Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", loadStepId,
								remoteIoTraceLogFileName, fileMgr
							);
						}
					}
				);
		} finally {
			progressOutputThread.interrupt();
			Loggers.MSG.info(
				"\"{}\": transferred {} of the operation traces data", loadStepId,
				SizeInBytes.formatFixedSize(byteCounter.longValue())
			);
		}
	}

	private static void transferToLocal(
		final FileManager fileMgr, final String remoteIoTraceLogFileName, final LongAdder byteCounter
	) {
		long transferredByteCount = 0;
		try(final Instance logCtx = put(KEY_CLASS_NAME, OpTraceLogFileAggregator.class.getSimpleName())) {
			byte[] data;
			while(true) {
				data = fileMgr.readFromFile(remoteIoTraceLogFileName, transferredByteCount);
				Loggers.OP_TRACES.info(new String(data));
				transferredByteCount += data.length;
				byteCounter.add(data.length);
			}
		} catch(final EOFException ok) {
		} catch(final RemoteException e) {
			LogUtil.exception(Level.WARN, e, "Failed to read the data from the remote file");
		} catch(final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected I/O exception");
		} finally {
			Loggers.MSG.debug(
				"Transferred {} of the remote operation traces data from the remote file \"{}\" @ \"{}\"",
				SizeInBytes.formatFixedSize(transferredByteCount), remoteIoTraceLogFileName, fileMgr
			);
		}
	}

	@Override
	public final void close() {
		collectToLocal();
		opTraceLogFileSlices.clear();
	}
}
