package com.emc.mongoose.storage.driver.coop.nio.fs;


import com.emc.mongoose.data.DataCorruptionException;
import com.emc.mongoose.data.DataSizeException;
import com.emc.mongoose.item.DataItem;
import static com.emc.mongoose.item.DataItem.rangeCount;
import static com.emc.mongoose.item.DataItem.rangeOffset;
import static com.emc.mongoose.storage.driver.coop.nio.fs.FsConstants.FS;
import static com.emc.mongoose.storage.driver.coop.nio.fs.FsConstants.FS_PROVIDER;
import static com.emc.mongoose.storage.driver.coop.nio.fs.FsConstants.READ_OPEN_OPT;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.io.task.data.DataIoTask;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.system.DirectMemUtil;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;

public interface FileIoHelper {

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeCreate(
		final I fileItem, final O ioTask, final FileChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.countBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize && IoTask.Status.ACTIVE.equals(ioTask.status())) {
			countBytesDone += fileItem.writeToFileChannel(dstChannel, contentSize - countBytesDone);
			ioTask.countBytesDone(countBytesDone);
		}
		return countBytesDone >= contentSize;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeCopy(
		final I fileItem, final O ioTask, final FileChannel srcChannel, final FileChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.countBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize && IoTask.Status.ACTIVE.equals(ioTask.status())) {
			countBytesDone += srcChannel.transferTo(
				countBytesDone, contentSize - countBytesDone, dstChannel
			);
			ioTask.countBytesDone(countBytesDone);
		}
		return countBytesDone >= contentSize;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeReadAndVerify(
		final I fileItem, final O ioTask, final ReadableByteChannel srcChannel
	) throws DataSizeException, DataCorruptionException, IOException {
		long countBytesDone = ioTask.countBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize) {
			if(fileItem.isUpdated()) {
				final DataItem currRange = ioTask.currRange();
				final int nextRangeIdx = ioTask.currRangeIdx() + 1;
				final long nextRangeOffset = rangeOffset(nextRangeIdx);
				if(currRange != null) {
					final ByteBuffer inBuff = DirectMemUtil.getThreadLocalReusableBuff(
						nextRangeOffset - countBytesDone
					);
					final int n = srcChannel.read(inBuff);
					if(n < 0) {
						throw new DataSizeException(contentSize, countBytesDone);
					} else {
						inBuff.flip();
						currRange.verify(inBuff);
						currRange.position(currRange.position() + n);
						countBytesDone += n;
						if(countBytesDone == nextRangeOffset) {
							ioTask.currRangeIdx(nextRangeIdx);
						}
					}
				} else {
					throw new AssertionError("Null data range");
				}
			} else {
				final ByteBuffer inBuff = DirectMemUtil.getThreadLocalReusableBuff(
					contentSize - countBytesDone
				);
				final int n = srcChannel.read(inBuff);
				if(n < 0) {
					throw new DataSizeException(contentSize, countBytesDone);
				} else {
					inBuff.flip();
					fileItem.verify(inBuff);
					fileItem.position(fileItem.position() + n);
					countBytesDone += n;
				}
			}
			ioTask.countBytesDone(countBytesDone);
		}

		return countBytesDone >= contentSize;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeReadAndVerifyRandomRanges(
		final I fileItem, final O ioTask, final SeekableByteChannel srcChannel,
		final BitSet maskRangesPair[]
	) throws DataSizeException, DataCorruptionException, IOException {

		long countBytesDone = ioTask.countBytesDone();
		final long rangesSizeSum = ioTask.markedRangesSize();

		if(rangesSizeSum > 0 && rangesSizeSum > countBytesDone) {

			DataItem range2read;
			int currRangeIdx;
			while(true) {
				currRangeIdx = ioTask.currRangeIdx();
				if(currRangeIdx < rangeCount(fileItem.size())) {
					if(maskRangesPair[0].get(currRangeIdx) || maskRangesPair[1].get(currRangeIdx)) {
						range2read = ioTask.currRange();
						if(Loggers.MSG.isTraceEnabled()) {
							Loggers.MSG.trace(
								"I/O task: {}, Range index: {}, size: {}, internal position: {}, " +
									"Done byte count: {}",
								ioTask.toString(), currRangeIdx, range2read.size(),
								range2read.position(), countBytesDone
							);
						}
						break;
					} else {
						ioTask.currRangeIdx(++ currRangeIdx);
					}
				} else {
					ioTask.countBytesDone(rangesSizeSum);
					return true;
				}
			}

			final long currRangeSize = range2read.size();
			final long currPos = rangeOffset(currRangeIdx) + countBytesDone;
			srcChannel.position(currPos);
			final ByteBuffer inBuff = DirectMemUtil.getThreadLocalReusableBuff(currRangeSize - countBytesDone);
			final int n = srcChannel.read(inBuff);
			if(n < 0) {
				throw new DataSizeException(rangesSizeSum, countBytesDone);
			} else {
				inBuff.flip();
				try {
					range2read.verify(inBuff);
					range2read.position(range2read.position() + n);
					countBytesDone += n;
				} catch(final DataCorruptionException e) {
					throw new DataCorruptionException(
						currPos + e.getOffset() - countBytesDone, e.expected, e.actual
					);
				}
			}

			if(Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace(
					"I/O task: {}, Done bytes count: {}, Curr range size: {}",
					ioTask.toString(), countBytesDone, range2read.size()
				);
			}

			if(countBytesDone == currRangeSize) {
				ioTask.currRangeIdx(currRangeIdx + 1);
				ioTask.countBytesDone(0);
			} else {
				ioTask.countBytesDone(countBytesDone);
			}
		}

		return rangesSizeSum <= 0 || rangesSizeSum <= countBytesDone;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeReadAndVerifyFixedRanges(
		final I fileItem, final O ioTask, final SeekableByteChannel srcChannel,
		final List<Range> fixedRanges
	) throws DataSizeException, DataCorruptionException, IOException {

		final long baseItemSize = fileItem.size();
		final long fixedRangesSizeSum = ioTask.markedRangesSize();

		long countBytesDone = ioTask.countBytesDone();
		// "countBytesDone" is the current range done bytes counter here
		long rangeBytesDone = countBytesDone;
		long currOffset;
		long cellOffset;
		long cellEnd;
		int n;

		if(fixedRangesSizeSum > 0 && fixedRangesSizeSum > countBytesDone) {

			Range fixedRange;
			DataItem currRange;
			int currFixedRangeIdx = ioTask.currRangeIdx();
			long fixedRangeEnd;
			long fixedRangeSize;

			if(currFixedRangeIdx < fixedRanges.size()) {
				fixedRange = fixedRanges.get(currFixedRangeIdx);
				currOffset = fixedRange.getBeg();
				fixedRangeEnd = fixedRange.getEnd();
				if(currOffset == -1) {
					// last "rangeEnd" bytes
					currOffset = baseItemSize - fixedRangeEnd;
					fixedRangeSize = fixedRangeEnd;
				} else if(fixedRangeEnd == -1) {
					// start @ offset equal to "rangeBeg"
					fixedRangeSize = baseItemSize - currOffset;
				} else {
					fixedRangeSize = fixedRangeEnd - currOffset + 1;
				}

				// let (current offset = rangeBeg + rangeBytesDone)
				currOffset += rangeBytesDone;
				// find the internal data item's cell index which has:
				// (cell's offset <= current offset) && (cell's end > current offset)
				n = rangeCount(currOffset + 1) - 1;
				cellOffset = rangeOffset(n);
				cellEnd = Math.min(baseItemSize, rangeOffset(n + 1));
				// get the found cell data item (updated or not)
				currRange = fileItem.slice(cellOffset, cellEnd - cellOffset);
				if(fileItem.isRangeUpdated(n)) {
					currRange.layer(fileItem.layer() + 1);
				}
				// set the cell data item internal position to (current offset - cell's offset)
				currRange.position(currOffset - cellOffset);
				srcChannel.position(currOffset);

				final ByteBuffer inBuff = DirectMemUtil.getThreadLocalReusableBuff(
					Math.min(fixedRangeSize - countBytesDone, currRange.size() - currRange.position())
				);
				final int m = srcChannel.read(inBuff);
				if(m < 0) {

				} else {
					inBuff.flip();
					try {
						currRange.verify(inBuff);
						currRange.position(currRange.position() + m);
						rangeBytesDone += m;
					} catch(final DataCorruptionException e) {
						throw new DataCorruptionException(
							currOffset + e.getOffset() - countBytesDone, e.expected, e.actual
						);
					}
				}

				if(rangeBytesDone == fixedRangeSize) {
					// current byte range verification is finished
					if(currFixedRangeIdx == fixedRanges.size() - 1) {
						// current byte range was last in the list
						ioTask.countBytesDone(fixedRangesSizeSum);
						return true;
					} else {
						ioTask.currRangeIdx(currFixedRangeIdx + 1);
						rangeBytesDone = 0;
					}
				}
				ioTask.countBytesDone(rangeBytesDone);
			} else {
				ioTask.countBytesDone(fixedRangesSizeSum);
			}
		}

		return fixedRangesSizeSum <= 0 || fixedRangesSizeSum <= countBytesDone;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeRead(
		final I fileItem, final O ioTask, final ReadableByteChannel srcChannel
	) throws IOException {
		long countBytesDone = ioTask.countBytesDone();
		final long contentSize = fileItem.size();
		int n;
		if(countBytesDone < contentSize) {
			n = srcChannel.read(
				DirectMemUtil.getThreadLocalReusableBuff(contentSize - countBytesDone)
			);
			if(n < 0) {
				ioTask.countBytesDone(countBytesDone);
				fileItem.size(countBytesDone);
				return true;
			} else {
				countBytesDone += n;
				ioTask.countBytesDone(countBytesDone);
			}
		}
		return countBytesDone == contentSize;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeReadRandomRanges(
		final I fileItem, final O ioTask, final FileChannel srcChannel,
		final BitSet maskRangesPair[]
	) throws IOException {

		int n;
		long countBytesDone = ioTask.countBytesDone();
		final long rangesSizeSum = ioTask.markedRangesSize();

		if(rangesSizeSum > 0 && rangesSizeSum > countBytesDone) {

			DataItem range2read;
			int currRangeIdx;
			while(true) {
				currRangeIdx = ioTask.currRangeIdx();
				if(currRangeIdx < rangeCount(fileItem.size())) {
					if(maskRangesPair[0].get(currRangeIdx) || maskRangesPair[1].get(currRangeIdx)) {
						range2read = ioTask.currRange();
						break;
					} else {
						ioTask.currRangeIdx(++ currRangeIdx);
					}
				} else {
					ioTask.countBytesDone(rangesSizeSum);
					return true;
				}
			}

			final long currRangeSize = range2read.size();
			n = srcChannel.read(
				DirectMemUtil.getThreadLocalReusableBuff(currRangeSize - countBytesDone),
				rangeOffset(currRangeIdx) + countBytesDone
			);
			if(n < 0) {
				ioTask.countBytesDone(countBytesDone);
				return true;
			}
			countBytesDone += n;

			if(countBytesDone == currRangeSize) {
				ioTask.currRangeIdx(currRangeIdx + 1);
				ioTask.countBytesDone(0);
			} else {
				ioTask.countBytesDone(countBytesDone);
			}
		}

		return rangesSizeSum <= 0 || rangesSizeSum <= countBytesDone;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeReadFixedRanges(
		final I fileItem, final O ioTask, final FileChannel srcChannel, final List<Range> byteRanges
	) throws IOException {

		int n;
		long countBytesDone = ioTask.countBytesDone();
		final long baseItemSize = fileItem.size();
		final long rangesSizeSum = ioTask.markedRangesSize();

		if(rangesSizeSum > 0 && rangesSizeSum > countBytesDone) {

			Range byteRange;
			int currRangeIdx = ioTask.currRangeIdx();
			long rangeBeg;
			long rangeEnd;
			long rangeSize;

			if(currRangeIdx < byteRanges.size()) {
				byteRange = byteRanges.get(currRangeIdx);
				rangeBeg = byteRange.getBeg();
				rangeEnd = byteRange.getEnd();
				if(rangeBeg == -1) {
					// last "rangeEnd" bytes
					rangeBeg = baseItemSize - rangeEnd;
					rangeSize = rangeEnd;
				} else if(rangeEnd == -1) {
					// start @ offset equal to "rangeBeg"
					rangeSize = baseItemSize - rangeBeg;
				} else {
					rangeSize = rangeEnd - rangeBeg + 1;
				}
				n = srcChannel.read(
					DirectMemUtil.getThreadLocalReusableBuff(rangeSize - countBytesDone),
					rangeBeg + countBytesDone
				);
				if(n < 0) {
					ioTask.countBytesDone(countBytesDone);
					return true;
				}
				countBytesDone += n;

				if(countBytesDone == rangeSize) {
					ioTask.currRangeIdx(currRangeIdx + 1);
					ioTask.countBytesDone(0);
				} else {
					ioTask.countBytesDone(countBytesDone);
				}
			} else {
				ioTask.countBytesDone(rangesSizeSum);
			}
		}

		return rangesSizeSum <= 0 || rangesSizeSum <= countBytesDone;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeRandomRangesUpdate(
		final I fileItem, final O ioTask, final FileChannel dstChannel
	) throws IOException {

		long countBytesDone = ioTask.countBytesDone();
		final long updatingRangesSize = ioTask.markedRangesSize();

		if(updatingRangesSize > 0 && updatingRangesSize > countBytesDone) {

			DataItem updatingRange;
			int currRangeIdx;
			while(true) {
				currRangeIdx = ioTask.currRangeIdx();
				if(currRangeIdx < rangeCount(fileItem.size())) {
					updatingRange = ioTask.currRangeUpdate();
					if(updatingRange == null) {
						ioTask.currRangeIdx(++ currRangeIdx);
					} else {
						break;
					}
				} else {
					ioTask.countBytesDone(updatingRangesSize);
					return true;
				}
			}

			final long updatingRangeSize = updatingRange.size();
			if(Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace(
					"{}: set the file position = {} + {}", fileItem.getName(),
					rangeOffset(currRangeIdx), countBytesDone
				);
			}
			dstChannel.position(rangeOffset(currRangeIdx) + countBytesDone);
			countBytesDone += updatingRange.writeToFileChannel(
				dstChannel, updatingRangeSize - countBytesDone
			);
			if(Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace(
					"{}: {} bytes written totally", fileItem.getName(), countBytesDone
				);
			}
			if(countBytesDone == updatingRangeSize) {
				ioTask.currRangeIdx(currRangeIdx + 1);
				ioTask.countBytesDone(0);
			} else {
				ioTask.countBytesDone(countBytesDone);
			}
		} else {
			fileItem.commitUpdatedRanges(ioTask.markedRangesMaskPair());
		}

		return updatingRangesSize <= 0 || updatingRangesSize <= countBytesDone;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeFixedRangesUpdate(
		final I fileItem, final O ioTask, final FileChannel dstChannel, final List<Range> byteRanges
	) throws IOException {

		long countBytesDone = ioTask.countBytesDone();
		final long baseItemSize = fileItem.size();
		final long updatingRangesSize = ioTask.markedRangesSize();

		if(updatingRangesSize > 0 && updatingRangesSize > countBytesDone) {

			Range byteRange;
			DataItem updatingRange;
			int currRangeIdx = ioTask.currRangeIdx();
			long rangeBeg;
			long rangeEnd;
			long rangeSize;

			if(currRangeIdx < byteRanges.size()) {
				byteRange = byteRanges.get(currRangeIdx);
				rangeBeg = byteRange.getBeg();
				rangeEnd = byteRange.getEnd();
				rangeSize = byteRange.getSize();
				if(rangeSize == -1) {
					if(rangeBeg == -1) {
						// last "rangeEnd" bytes
						rangeBeg = baseItemSize - rangeEnd;
						rangeSize = rangeEnd;
					} else if(rangeEnd == -1) {
						// start @ offset equal to "rangeBeg"
						rangeSize = baseItemSize - rangeBeg;
					} else {
						rangeSize = rangeEnd - rangeBeg + 1;
					}
				} else {
					// append
					rangeBeg = baseItemSize;
					// note down the new size
					fileItem.size(baseItemSize + updatingRangesSize);
				}
				updatingRange = fileItem.slice(rangeBeg, rangeSize);
				updatingRange.position(countBytesDone);
				dstChannel.position(rangeBeg + countBytesDone);
				countBytesDone += updatingRange.writeToFileChannel(dstChannel, rangeSize - countBytesDone);

				if(countBytesDone == rangeSize) {
					ioTask.currRangeIdx(currRangeIdx + 1);
					ioTask.countBytesDone(0);
				} else {
					ioTask.countBytesDone(countBytesDone);
				}

			} else {
				ioTask.countBytesDone(updatingRangesSize);
			}
		}

		return updatingRangesSize <= 0 || updatingRangesSize <= countBytesDone;
	}

	static <I extends DataItem, O extends DataIoTask<I>> boolean invokeOverwrite(
		final I fileItem, final O ioTask, final FileChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.countBytesDone();
		if(countBytesDone == 0) {
			dstChannel.position(countBytesDone);
		}
		final long fileSize = fileItem.size();
		if(countBytesDone < fileSize && IoTask.Status.ACTIVE.equals(ioTask.status())) {
			countBytesDone += fileItem.writeToFileChannel(dstChannel, fileSize - countBytesDone);
			ioTask.countBytesDone(countBytesDone);
		}
		return countBytesDone >= fileSize;
	}

	static <I extends DataItem, O extends DataIoTask<I>> FileChannel openSrcFile(final O ioTask) {
		final String srcPath = ioTask.srcPath();
		if(srcPath == null || srcPath.isEmpty()) {
			return null;
		}
		final String fileItemName = ioTask.item().getName();
		final Path srcFilePath = fileItemName.startsWith(srcPath) ?
			FS.getPath(fileItemName) : FS.getPath(srcPath, fileItemName);
		try {
			return FS_PROVIDER.newFileChannel(srcFilePath, READ_OPEN_OPT);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to open the source channel for the path @ \"{}\"",
				srcFilePath
			);
			ioTask.status(IoTask.Status.FAIL_IO);
			return null;
		}
	}
}
