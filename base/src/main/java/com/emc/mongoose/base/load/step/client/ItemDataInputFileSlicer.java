package com.emc.mongoose.base.load.step.client;

import com.emc.mongoose.base.load.step.file.FileManager;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.confuse.Config;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;

import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

public final class ItemDataInputFileSlicer implements AutoCloseable {

	private final String loadStepId;
	private final Map<FileManager, String> itemDataInputFileSlices;

	public ItemDataInputFileSlicer(
					final String loadStepId,
					final List<FileManager> fileMgrs,
					final List<Config> configSlices,
					final String itemDataInputFile,
					final int batchSize) {
		this.loadStepId = loadStepId;
		final var sliceCount = configSlices.size();
		itemDataInputFileSlices = new HashMap<>(sliceCount);
		for (var i = 0; i < sliceCount; i++) {
			try {
				final var fileMgr = fileMgrs.get(i);
				final var itemDataInputFileName = fileMgr.newTmpFileName();
				itemDataInputFileSlices.put(fileMgr, itemDataInputFileName);
				final var configSlice = configSlices.get(i);
				configSlice.val("item-data-input-file", itemDataInputFileName);
			} catch (final Exception e) {
				LogUtil.exception(
								Level.ERROR, e, "Failed to get the item data input file name for the step slice #" + i);
			}
		}

		try {
			Loggers.MSG.info(
							"{}: distribute the data from the input file \"{}\"...", loadStepId, itemDataInputFile);
			distributeData(itemDataInputFile, batchSize);
		} catch (final IOException e) {
			LogUtil.exception(Level.WARN, e, "{}: failed to use the item input", loadStepId);
		} catch (final Throwable cause) {
			throwUncheckedIfInterrupted(cause);
			LogUtil.exception(Level.ERROR, cause, "{}: unexpected failure", loadStepId);
		}
	}

	@Override
	public final void close() throws Exception {
		itemDataInputFileSlices
						.entrySet()
						.parallelStream()
						.forEach(
										entry -> {
											final var fileMgr = entry.getKey();
											final var itemDataInputFileName = entry.getValue();
											try {
												fileMgr.deleteFile(itemDataInputFileName);
											} catch (final Exception e) {
												throwUncheckedIfInterrupted(e);
												LogUtil.exception(
																Level.WARN,
																e,
																"{}: failed to delete the file \"{}\" @ file manager \"{}\"",
																loadStepId,
																itemDataInputFileName,
																fileMgr);
											}
										});
		itemDataInputFileSlices.clear();
	}

	private void distributeData(final String itemDataInputFile, final int batchSize)
					throws IOException {
		final ByteBuffer bBuff = ByteBuffer.allocate(batchSize);
		int n;
		try (final SeekableByteChannel itemDataInput = Files.newByteChannel(Paths.get(itemDataInputFile))) {
			while (0 < (n = itemDataInput.read(bBuff))) {
				final byte[] buff = new byte[n];
				bBuff.flip();
				bBuff.get(buff);
				itemDataInputFileSlices
								.entrySet()
								.parallelStream()
								.forEach(
												entry -> {
													final FileManager fileMgr = entry.getKey();
													final String itemDataInputFileName = entry.getValue();
													try {
														fileMgr.writeToFile(itemDataInputFileName, buff);
													} catch (final IOException e) {
														LogUtil.exception(
																		Level.ERROR,
																		e,
																		"Failed to write the data to the slice file \"{}\"",
																		itemDataInputFile);
													}
												});
			}
		}
	}
}
