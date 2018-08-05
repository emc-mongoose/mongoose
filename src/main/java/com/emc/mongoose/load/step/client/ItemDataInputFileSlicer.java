package com.emc.mongoose.load.step.client;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.load.step.file.FileManager;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemDataInputFileSlicer
implements AutoCloseable {

	private final String loadStepId;
	private final Map<FileManager, String> itemDataInputFileSlices;
	private final List<FileManager> fileMgrs;

	public ItemDataInputFileSlicer(
		final String loadStepId, final List<FileManager> fileMgrs, final List<Config> configSlices,
		final String itemDataInputFile, final int batchSize
	) {
		this.loadStepId = loadStepId;
		final int sliceCount = configSlices.size();
		itemDataInputFileSlices = new HashMap<>(sliceCount);
		this.fileMgrs = fileMgrs;
		for(int i = 0; i < sliceCount; i ++) {
			try {
				final FileManager fileMgr = fileMgrs.get(i);
				final String itemDataInputFileName = fileMgr.newTmpFileName();
				itemDataInputFileSlices.put(fileMgr, itemDataInputFileName);
				final Config configSlice = configSlices.get(i);
				configSlice.val("item-data-input-file", itemDataInputFileName);
			} catch(final Exception e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to get the item data input file name for the step slice #" + i
				);
			}
		}

		try {
			Loggers.MSG.info("{}: distribute the data from the input file \"{}\"...", loadStepId, itemDataInputFile);
			distributeData(itemDataInputFile, batchSize);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "{}: failed to use the item input", loadStepId);
		} catch(final InterruptRunException e) {
			throw e;
		} catch(final Throwable cause) {
			LogUtil.exception(Level.ERROR, cause, "{}: unexpected failure", loadStepId);
		}
	}

	@Override
	public final void close()
	throws Exception {
		itemDataInputFileSlices
			.entrySet()
			.parallelStream()
			.forEach(
				entry -> {
					final FileManager fileMgr = entry.getKey();
					final String itemDataInputFileName = entry.getValue();
					try {
						fileMgr.deleteFile(itemDataInputFileName);
					} catch(final Exception e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", loadStepId,
							itemDataInputFileName, fileMgr
						);
					}
				}
			);
		itemDataInputFileSlices.clear();
	}

	private void distributeData(final String itemDataInputFile, final int batchSize)
	throws IOException {

	}
}
