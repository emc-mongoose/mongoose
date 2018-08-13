package com.emc.mongoose.load.step.client;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.load.step.file.FileManager;
import com.emc.mongoose.load.step.service.file.FileManagerService;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.load.step.client.LoadStepClient.OUTPUT_PROGRESS_PERIOD_MILLIS;
import static com.emc.mongoose.load.step.file.FileManager.APPEND_OPEN_OPTIONS;

import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ItemOutputFileAggregator
implements AutoCloseable {

	private final String loadStepId;
	private final String itemOutputFile;
	private final Map<FileManager, String> itemOutputFileSlices;

	public ItemOutputFileAggregator(
		final String loadStepId, final List<FileManager> fileMgrs, final List<Config> configSlices,
		final String itemOutputFile
	) {
		this.loadStepId = loadStepId;
		this.itemOutputFile = itemOutputFile;
		final int sliceCount = fileMgrs.size();
		this.itemOutputFileSlices = new HashMap<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			final FileManager fileMgr = fileMgrs.get(i);
			if(i == 0) {
				if(fileMgr instanceof FileManagerService) {
					throw new AssertionError("File manager @ index #" + i + " shouldn't be a service");
				}
				itemOutputFileSlices.put(fileMgr, itemOutputFile);
			} else {
				if(fileMgr instanceof FileManagerService) {
					try {
						final String remoteItemOutputFileName = fileMgr.newTmpFileName();
						configSlices.get(i).val("item-output-file", remoteItemOutputFileName);
						itemOutputFileSlices.put(fileMgr, remoteItemOutputFileName);
						Loggers.MSG.debug("\"{}\": new tmp item output file \"{}\"", fileMgr, remoteItemOutputFileName);
					} catch(final Exception e) {
						LogUtil.exception(
							Level.ERROR, e,
							"Failed to get the new temporary file name for the file manager service \"{}\"", fileMgr
						);
					}
				} else {
					throw new AssertionError("File manager @ index #" + i + " should be a service");
				}
			}
		}
	}

	@Override
	public final void close() {
		try {
			collectToLocal();
		} finally {
			itemOutputFileSlices.clear();
		}
	}

	private void collectToLocal() {
		final LongAdder byteCounter = new LongAdder();
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
			2, new LogContextThreadFactory("collectItemOutputFileWorker", true)
		);
		final CountDownLatch finishLatch = new CountDownLatch(1);
		executor.submit(
			() -> {
				try(
					final OutputStream localItemOutput = Files.newOutputStream(
						Paths.get(itemOutputFile), APPEND_OPEN_OPTIONS
					)
				) {
					final Lock localItemOutputLock = new ReentrantLock();
					itemOutputFileSlices
						.entrySet()
						.parallelStream()
						// don't transfer & delete local item output file
						.filter(entry -> entry.getKey() instanceof FileManagerService)
						.forEach(
							entry -> {
								final FileManager fileMgr = entry.getKey();
								final String remoteItemOutputFileName = entry.getValue();
								transferToLocal(
									fileMgr, remoteItemOutputFileName, localItemOutput, localItemOutputLock, byteCounter
								);
								try {
									fileMgr.deleteFile(remoteItemOutputFileName);
								} catch(final Exception e) {
									LogUtil.exception(
										Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"",
										loadStepId, remoteItemOutputFileName, fileMgr
									);
								}
							}
						);
				} catch(final IOException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to open the local item output file \"{}\" for appending", loadStepId,
						itemOutputFile
					);
				} finally {
					finishLatch.countDown();
				}
			}
		);
		executor.scheduleAtFixedRate(
			() -> Loggers.MSG.info(
				"\"{}\" <- transferred {} of the output items data...", itemOutputFile,
				SizeInBytes.formatFixedSize(byteCounter.longValue())
			),
			0, OUTPUT_PROGRESS_PERIOD_MILLIS, TimeUnit.MILLISECONDS
		);

		try {
			finishLatch.await();
		} catch(final InterruptedException e) {
			throw new InterruptRunException(e);
		} finally {
			executor.shutdownNow();
			Loggers.MSG.info(
				"\"{}\" <- transferred {} of the output items data", itemOutputFile,
				SizeInBytes.formatFixedSize(byteCounter.longValue())
			);
		}
	}

	private static void transferToLocal(
		final FileManager fileMgr, final String remoteItemOutputFileName, final OutputStream localItemOutput,
		final Lock localItemOutputLock, final LongAdder byteCounter
	) {
		long transferredByteCount = 0;
		try(final Instance logCtx = put(KEY_CLASS_NAME, ItemOutputFileAggregator.class.getSimpleName())) {
			byte buff[];
			while(true) {
				buff = fileMgr.readFromFile(remoteItemOutputFileName, transferredByteCount);
				localItemOutputLock.lock();
				try {
					localItemOutput.write(buff);
				} finally {
					localItemOutputLock.unlock();
				}
				transferredByteCount += buff.length;
				byteCounter.add(buff.length);
			}
		} catch(final EOFException ok) {
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Remote items output file transfer failure");
		} finally {
			Loggers.MSG.debug(
				"{} of items output data transferred from \"{}\" @ \"{}\" to \"{}\"",
				SizeInBytes.formatFixedSize(transferredByteCount), remoteItemOutputFileName, fileMgr, localItemOutput
			);
		}
	}
}
