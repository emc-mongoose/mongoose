package com.emc.mongoose.base.load.step.client;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.emc.mongoose.base.load.step.client.LoadStepClient.OUTPUT_PROGRESS_PERIOD_MILLIS;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import com.emc.mongoose.base.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.base.load.step.file.FileManager;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.fiber4j.ExclusiveFiberBase;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.Level;

public final class TempInputTextFileSlicer implements AutoCloseable {

	private final String loadStepId;
	private final Map<FileManager, String> fileSlices;

	public TempInputTextFileSlicer(
					final String loadStepId,
					final String srcFileName,
					final List<FileManager> fileMgrs,
					final String configPath,
					final List<Config> configSlices,
					final int batchSize) {
		this.loadStepId = loadStepId;
		final var sliceCount = configSlices.size();
		fileSlices = new HashMap<>(sliceCount);
		for (var i = 0; i < sliceCount; i++) {
			try {
				final var fileMgr = fileMgrs.get(i);
				final var fileName = fileMgr.newTmpFileName();
				fileSlices.put(fileMgr, fileName);
				final var configSlice = configSlices.get(i);
				configSlice.val(configPath, fileName);
			} catch (final Exception e) {
				LogUtil.exception(
								Level.ERROR, e, "Failed to get the input text file name for the step slice #" + i);
			}
		}

		try (final var logCtx = put(KEY_STEP_ID, loadStepId).put(KEY_CLASS_NAME, getClass().getSimpleName())) {
			Loggers.MSG.info(
							"{}: scatter the lines from the input text file \"{}\"...", loadStepId, srcFileName);
			scatterLines(srcFileName, sliceCount, fileMgrs, fileSlices, batchSize);
			Loggers.MSG.info(
							"{}: scatter the lines from the input text file \"{}\" finished",
							loadStepId,
							srcFileName);
		} catch (final Throwable cause) {
			throwUncheckedIfInterrupted(cause);
			LogUtil.exception(
							Level.ERROR,
							cause,
							"{}: failed to scatter the lines from the file \"{}\"",
							loadStepId,
							srcFileName);
		}
	}

	@Override
	public final void close() {
		fileSlices
						.entrySet()
						.parallelStream()
						.forEach(
										entry -> {
											final var fileMgr = entry.getKey();
											final var fileName = entry.getValue();
											try {
												fileMgr.deleteFile(fileName);
											} catch (final Exception e) {
												throwUncheckedIfInterrupted(e);
												LogUtil.exception(
																Level.WARN,
																e,
																"{}: failed to delete the file \"{}\" @ file manager \"{}\"",
																loadStepId,
																fileName,
																fileMgr);
											}
										});
		fileSlices.clear();
	}

	static void scatterLines(
					final String srcFileName,
					final int sliceCount,
					final List<FileManager> fileMgrs,
					final Map<FileManager, String> fileSlices,
					final int batchSize)
					throws IOException {

		final var inputFinishFlag = new AtomicBoolean(false);
		final List<BlockingQueue<String>> lineQueues = new ArrayList<>(sliceCount);
		for (var i = 0; i < sliceCount; i++) {
			lineQueues.add(new ArrayBlockingQueue<>(batchSize));
		}

		final List<AsyncRunnable> tasks = new ArrayList<>(sliceCount + 1);
		tasks.add(new ReadTask(inputFinishFlag, lineQueues, srcFileName, sliceCount));

		final var writeFinishCountDown = new CountDownLatch(sliceCount);
		for (var i = 0; i < sliceCount; i++) {
			final var lineQueue = lineQueues.get(i);
			final var fileMgr = fileMgrs.get(i);
			final var dstFileName = fileSlices.get(fileMgr);
			tasks.add(
							new WriteTask(
											inputFinishFlag, writeFinishCountDown, lineQueue, fileMgr, dstFileName, batchSize));
		}

		tasks.forEach(
						task -> {
							try {
								task.start();
							} catch (final RemoteException ignored) {}
						});

		try {
			writeFinishCountDown.await();
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} finally {
			tasks.forEach(
							task -> {
								try {
									task.close();
								} catch (final IOException ignored) {}
							});
			tasks.clear();
		}
	}

	private static final class ReadTask extends ExclusiveFiberBase {

		private final AtomicBoolean inputFinishFlag;
		private final List<BlockingQueue<String>> lineQueues;
		private final String srcFileName;
		private final int sliceCount;
		private final BufferedReader lineReader;

		private long lineCount = 0;
		private long lastProgressOutputTimeMillis = System.currentTimeMillis();
		private volatile String pendingLine = null;

		public ReadTask(
						final AtomicBoolean inputFinishFlag,
						final List<BlockingQueue<String>> lineQueues,
						final String srcFileName,
						final int sliceCount)
						throws IOException {

			super(ServiceTaskExecutor.INSTANCE);

			this.inputFinishFlag = inputFinishFlag;
			this.lineQueues = lineQueues;
			this.srcFileName = srcFileName;
			this.sliceCount = sliceCount;

			lineReader = Files.newBufferedReader(Paths.get(srcFileName));
		}

		@Override
		protected final void invokeTimedExclusively(final long startTimeNanos) {
			try {
				String line;

				while (System.nanoTime() - startTimeNanos < SOFT_DURATION_LIMIT_NANOS) {

					if (System.currentTimeMillis() - lastProgressOutputTimeMillis > OUTPUT_PROGRESS_PERIOD_MILLIS) {
						Loggers.MSG.info(
										"Read task progress: scattered {} lines from the input file \"{}\"...",
										lineCount,
										srcFileName);
						lastProgressOutputTimeMillis = System.currentTimeMillis();
					}

					if (pendingLine == null) {
						line = lineReader.readLine();
					} else {
						line = pendingLine;
						pendingLine = null;
					}

					if (line == null) {
						stop();
						break;
					} else {
						// shouldn't block because it may be the only available thread for the fibers execution
						// otherwise it may hang
						if (lineQueues.get((int) (lineCount % sliceCount)).offer(line)) {
							lineCount++;
						} else {
							pendingLine = line;
							break;
						}
					}
				}

			} catch (final IOException e) {
				LogUtil.exception(
								Level.WARN, e, "Read task failure, source file name: \"{}\"", srcFileName);
				stop();
			}
		}

		@Override
		protected final void doStop() {
			super.doStop();
			inputFinishFlag.set(true);
			Loggers.MSG.info(
							"Read task finish: scattered {} lines from the input file \"{}\"",
							lineCount,
							srcFileName);
		}

		@Override
		protected final void doClose() throws IOException {
			super.doClose();
			lineReader.close();
		}
	}

	private static final class WriteTask extends ExclusiveFiberBase {

		private final AtomicBoolean inputFinishFlag;
		private final CountDownLatch writeFinishCountDown;
		private final BlockingQueue<String> lineQueue;
		private final FileManager fileMgr;
		private final String dstFileName;
		private final int batchSize;
		private final List<String> lines;
		private final ByteArrayOutputStream linesByteBuff;
		private final BufferedWriter linesWriter;

		private long lineCount = 0;

		public WriteTask(
						final AtomicBoolean inputFinishFlag,
						final CountDownLatch writeFinishCountDown,
						final BlockingQueue<String> lineQueue,
						final FileManager fileMgr,
						final String dstFileName,
						final int batchSize) {
			super(ServiceTaskExecutor.INSTANCE);

			this.inputFinishFlag = inputFinishFlag;
			this.writeFinishCountDown = writeFinishCountDown;
			this.lineQueue = lineQueue;
			this.fileMgr = fileMgr;
			this.dstFileName = dstFileName;
			this.batchSize = batchSize;

			lines = new ArrayList<>(batchSize);
			linesByteBuff = new ByteArrayOutputStream();
			linesWriter = new BufferedWriter(new OutputStreamWriter(linesByteBuff));
		}

		@Override
		protected final void invokeTimedExclusively(final long startTimeNanos) {
			final var n = lineQueue.drainTo(lines, batchSize);
			if (n == 0 && inputFinishFlag.get()) {
				stop();
			} else {
				try {
					for (var i = 0; i < n; i++) {
						linesWriter.write(lines.get(i));
						linesWriter.newLine();
					}
					linesWriter.flush();
					fileMgr.writeToFile(dstFileName, linesByteBuff.toByteArray());
					lineCount += n;
				} catch (final IOException e) {
					LogUtil.exception(
									Level.WARN,
									e,
									"Write task failure, destination file name: \"{}\", file manager: \"{}\"",
									dstFileName,
									fileMgr);
					stop();
				}
				linesByteBuff.reset();
				lines.clear();
			}
		}

		@Override
		protected final void doStop() {
			writeFinishCountDown.countDown();
			Loggers.MSG.debug(
							"Write task finish, written line count: {}, destination file name: \"{}\", file manager: \"{}\"",
							lineCount,
							dstFileName,
							fileMgr);
		}

		@Override
		protected final void doClose() throws IOException {
			super.doClose();
			lines.clear();
			linesByteBuff.close();
			linesWriter.close();
		}
	}
}
