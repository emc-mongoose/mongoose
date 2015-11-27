package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.DataCorruptionException;
import com.emc.mongoose.core.api.data.DataSizeException;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.FileIOTask;
//
import com.emc.mongoose.core.impl.data.BasicDataItem;
import com.emc.mongoose.core.impl.data.BasicMutableDataItem;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIOTask<
	T extends FileItem, C extends Directory<T>, X extends FileIOConfig<T, C>
> extends BasicDataIOTask<T, C, X>
implements FileIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Path fPath;
	private final OpenOption openOption;
	//
	public BasicFileIOTask(final T item, final X ioConfig) {
		super(item, null, ioConfig);
		//
		fPath = Paths.get(ioConfig.getNamePrefix(), item.getName());
		//
		switch(ioType) {
			case CREATE:
				openOption = StandardOpenOption.CREATE_NEW;
				break;
			case READ:
				openOption = StandardOpenOption.READ;
				break;
			case DELETE:
				openOption = null;
				break;
			case UPDATE:
				openOption = StandardOpenOption.WRITE;
				break;
			case APPEND:
				openOption = StandardOpenOption.APPEND;
				break;
			default:
				openOption = null;
		}
	}
	//
	@Override
	public void run() {
		item.reset();
		if(openOption == null) { // delete
			runDelete();
		} else { // work w/ a content
			try(final SeekableByteChannel byteChannel = Files.newByteChannel(fPath, openOption)) {
				if(openOption == StandardOpenOption.READ) {
					runRead(byteChannel);
				} else {
					if(item.hasScheduledUpdates()) {
						runWriteUpdatedRanges(byteChannel);
					} else if(item.isAppending()) {
						runAppend(byteChannel);
					} else {
						runWriteFully(byteChannel);
					}
				}
			} catch(final IOException e) {
				status = Status.FAIL_IO;
				LogUtil.exception(
					LOG, Level.WARN, e,
					"Failed to {} the file \"{}\"", ioType.name().toLowerCase(), fPath
				);
			}
		}
	}
	//
	protected void runDelete() {
		try {
			Files.delete(fPath);
		} catch(final NoSuchFileException e) {
			status = Status.RESP_FAIL_NOT_FOUND;
		} catch(final IOException e) {
			status = Status.FAIL_IO;
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to {} the file \"{}\"", ioType.name().toLowerCase(), fPath
			);
		}
	}
	//
	protected void runRead(final SeekableByteChannel byteChannel)
	throws IOException {
		try {
			//
			int n;
			ByteBuffer buffIn;
			//
			if(ioConfig.getVerifyContentFlag()) {
				if(item.hasBeenUpdated()) {
					final int rangeCount = item.getCountRangesTotal();
					for(currRangeIdx = 0; currRangeIdx < rangeCount; currRangeIdx ++) {
						// prepare the byte range to read
						currRangeSize = item.getRangeSize(currRangeIdx);
						if(item.isCurrLayerRangeUpdated(currRangeIdx)) {
							currRange = new BasicDataItem(
								item.getOffset() + nextRangeOffset, currRangeSize,
								currDataLayerIdx + 1, ioConfig.getContentSource()
							);
						} else {
							currRange = new BasicDataItem(
								item.getOffset() + nextRangeOffset, currRangeSize,
								currDataLayerIdx, ioConfig.getContentSource()
							);
						}
						nextRangeOffset = BasicMutableDataItem.getRangeOffset(currRangeIdx);
						// read the bytes range
						if(currRangeSize > 0) {
							while(countBytesDone < contentSize) {
								buffIn = ((IOWorker) Thread.currentThread())
									.getThreadLocalBuff(nextRangeOffset - countBytesDone);
								n = currRange.readAndVerify(byteChannel, buffIn);
								if(n < 0) {
									break;
								} else {
									countBytesDone += n;
								}
							}
						}
					}
				} else {
					while(countBytesDone < contentSize) {
						buffIn = ((IOWorker) Thread.currentThread())
							.getThreadLocalBuff(contentSize - countBytesDone);
						n = item.readAndVerify(byteChannel, buffIn);
						if(n < 0) {
							break;
						} else {
							countBytesDone += n;
						}
					}
				}
			} else {
				while(countBytesDone < contentSize) {
					buffIn = ((IOWorker) Thread.currentThread())
						.getThreadLocalBuff(contentSize - countBytesDone);
					n = byteChannel.read(buffIn);
					if(n < 0) {
						break;
					} else {
						countBytesDone += n;
					}
				}
			}
		} catch(final DataSizeException e) {
			countBytesDone += e.offset;
			LOG.warn(
				Markers.MSG,
				"{}: content size mismatch, expected: {}, actual: {}",
				item.getName(), item.getSize(), e.offset
			);
			status = Status.RESP_FAIL_CORRUPT;
		} catch(final DataCorruptionException e) {
			countBytesDone += e.offset;
			LOG.warn(
				Markers.MSG,
				"{}: content mismatch @ offset {}, expected: {}, actual: {}",
				item.getName(), e.offset,
				String.format(
					"\"0x%X\"", e.expected), String.format("\"0x%X\"", e.actual
				)
			);
			status = Status.RESP_FAIL_CORRUPT;
		}
	}
	//
	protected void runWriteFully(final SeekableByteChannel byteChannel)
	throws IOException {
		while(countBytesDone < contentSize) {
			countBytesDone += item.write(byteChannel, contentSize - countBytesDone);
		}
		item.resetUpdates();
	}
	//
	protected void runWriteCurrRange(final SeekableByteChannel byteChannel)
	throws IOException {
		long n = 0;
		while(n < currRangeSize) {
			n += currRange.write(byteChannel, currRangeSize - n);
		}
		countBytesDone += n;
	}
	//
	protected void runWriteUpdatedRanges(final SeekableByteChannel byteChannel)
	throws IOException {
		final int rangeCount = item.getCountRangesTotal();
		final ContentSource contentSource = ioConfig.getContentSource();
		for(currRangeIdx = 0; currRangeIdx < rangeCount; currRangeIdx ++) {
			if(item.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRangeSize = item.getRangeSize(currRangeIdx);
				currRange = new BasicDataItem(
					item.getOffset() + nextRangeOffset, currRangeSize,
					currDataLayerIdx + 1, contentSource
				);
				runWriteCurrRange(byteChannel);
			}
		}
	}
	//
	protected void runAppend(final SeekableByteChannel byteChannel)
	throws IOException {
		final long prevSize = item.getSize();
		currRangeIdx = prevSize > 0 ? BasicMutableDataItem.getRangeCount(prevSize) - 1 : 0;
		if(item.isCurrLayerRangeUpdated(currRangeIdx)) {
			currRange = new BasicDataItem(
				item.getOffset() + prevSize, contentSize, currDataLayerIdx + 1,
				ioConfig.getContentSource()
			);
		} else {
			currRange = new BasicDataItem(
				item.getOffset() + prevSize, contentSize, currDataLayerIdx,
				ioConfig.getContentSource()
			);
		}
		//
		runWriteCurrRange(byteChannel);
	}
}
