package com.emc.mongoose.storage.driver.net.base.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.io.ThreadLocalByteBuffer;
import com.emc.mongoose.model.data.DataCorruptionException;
import com.emc.mongoose.model.data.DataSizeException;
import com.emc.mongoose.model.data.DataVerificationException;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.DataItem;
import static com.emc.mongoose.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.model.item.DataItem.getRangeOffset;

import com.emc.mongoose.ui.log.Loggers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

/**
 Created by andrey on 10.02.17.
 */
public abstract class ResponseContentUtil {

	public static void verifyChunk(
		final DataIoTask dataIoTask, final long countBytesDone, final ByteBuf contentChunk,
		final int chunkSize
	) throws IOException {
		final List<ByteRange> byteRanges = dataIoTask.getFixedRanges();
		final DataItem item = dataIoTask.getItem();
		try {
			if(byteRanges != null && !byteRanges.isEmpty()) {
				verifyChunkDataAndSize(
					dataIoTask, item, countBytesDone, contentChunk, chunkSize, byteRanges
				);
			} else if(dataIoTask.hasMarkedRanges()) {
				verifyChunkDataAndSize(
					dataIoTask, item, countBytesDone, contentChunk, chunkSize,
					dataIoTask.getMarkedRangesMaskPair()
				);
			} else {
				if(item.isUpdated()){
					verifyChunkUpdatedData(
						dataIoTask, item, countBytesDone, contentChunk, chunkSize
					);
				} else {
					if(chunkSize > item.size() - countBytesDone) {
						throw new DataSizeException(item.size(), countBytesDone + chunkSize);
					}
					verifyChunkData(item, contentChunk, 0, chunkSize);
				}
				dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
			}
		} catch(final DataVerificationException e) {
			final DataItem dataItem = dataIoTask.getItem();
			dataIoTask.setCountBytesDone(e.getOffset());
			dataIoTask.setStatus(IoTask.Status.RESP_FAIL_CORRUPT);
			if(e instanceof DataSizeException) {
				try {
					Loggers.MSG.debug(
						"{}: invalid size, expected: {}, actual: {} ",
						dataItem.getName(), dataItem.size(), e.getOffset()
					);
				} catch(final IOException ignored) {
				}
			} else if(e instanceof DataCorruptionException) {
				final DataCorruptionException ee = (DataCorruptionException) e;
				Loggers.MSG.debug(
					"{}: content mismatch @ offset {}, expected: {}, actual: {} ",
					dataItem.getName(), ee.getOffset(),
					String.format("\"0x%X\"", (int) (ee.expected & 0xFF)),
					String.format("\"0x%X\"", (int) (ee.actual & 0xFF))
				);
			}
		}
	}

	private static void verifyChunkDataAndSize(
		final DataIoTask dataIoTask, final DataItem dataItem, final long countBytesDone,
		final ByteBuf chunkData, final int chunkSize, final List<ByteRange> byteRanges
	) throws DataCorruptionException, IOException {

		final long rangesSizeSum = dataIoTask.getMarkedRangesSize();
		if(chunkSize > rangesSizeSum - countBytesDone) {
			throw new DataSizeException(
				dataIoTask.getMarkedRangesSize(), countBytesDone + chunkSize
			);
		}
		final long baseItemSize = dataItem.size();

		ByteRange byteRange;
		DataItem currRange;
		// "countBytesDone" is the current range done bytes counter here
		long rangeBytesDone = countBytesDone;
		long currOffset;
		long rangeEnd;
		long rangeSize;
		long cellOffset;
		long cellEnd;
		int currRangeIdx;
		int chunkOffset = 0;
		int n;

		while(chunkOffset < chunkSize) {
			currRangeIdx = dataIoTask.getCurrRangeIdx();
			byteRange = byteRanges.get(currRangeIdx);
			currOffset = byteRange.getBeg();
			rangeEnd = byteRange.getEnd();
			if(currOffset == -1) {
				// last "rangeEnd" bytes
				currOffset = baseItemSize - rangeEnd;
				rangeSize = rangeEnd;
			} else if(rangeEnd == -1) {
				// start @ offset equal to "currOffset"
				rangeSize = baseItemSize - currOffset;
			} else {
				rangeSize = rangeEnd - currOffset + 1;
			}

			// let (current offset = rangeBeg + rangeBytesDone)
			currOffset += rangeBytesDone;
			// find the internal data item's cell index which has:
			// (cell's offset <= current offset) && (cell's end > current offset)
			n = getRangeCount(currOffset + 1) - 1;
			cellOffset = getRangeOffset(n);
			cellEnd = Math.min(baseItemSize, getRangeOffset(n + 1));
			// get the found cell data item (updated or not)
			currRange = dataItem.slice(cellOffset, cellEnd - cellOffset);
			if(dataItem.isRangeUpdated(n)) {
				currRange.layer(dataItem.layer() + 1);
			}
			// set the cell data item internal position to (current offset - cell's offset)
			currRange.position(currOffset - cellOffset);
			n = (int) Math.min(
				chunkSize - chunkOffset,
				Math.min(rangeSize - rangeBytesDone, cellEnd - currOffset)
			);

			verifyChunkData(currRange, chunkData, chunkOffset, n);
			chunkOffset += n;
			rangeBytesDone += n;

			if(rangeBytesDone == rangeSize) {
				// current byte range verification is finished
				if(currRangeIdx == byteRanges.size() - 1) {
					// current byte range was last in the list
					dataIoTask.setCountBytesDone(rangesSizeSum);
					break;
				} else {
					dataIoTask.setCurrRangeIdx(currRangeIdx + 1);
					rangeBytesDone = 0;
				}
			}
			dataIoTask.setCountBytesDone(rangeBytesDone);
		}
	}

	private static void verifyChunkDataAndSize(
		final DataIoTask dataIoTask, final DataItem dataItem, final long countBytesDone,
		final ByteBuf chunkData, final int chunkSize, final BitSet markedRangesMaskPair[]
	) throws DataCorruptionException, IOException {
		if(chunkSize > dataIoTask.getMarkedRangesSize() - countBytesDone) {
			throw new DataSizeException(
				dataIoTask.getMarkedRangesSize(), countBytesDone + chunkSize
			);
		}

		DataItem currRange;
		// "countBytesDone" is the current range done bytes counter here
		long rangeBytesDone = countBytesDone;
		long rangeSize;
		int currRangeIdx;
		int chunkOffset = 0;
		int n;

		while(chunkOffset < chunkSize) {
			currRangeIdx = dataIoTask.getCurrRangeIdx();
			if(
				!markedRangesMaskPair[0].get(currRangeIdx) &&
					!markedRangesMaskPair[1].get(currRangeIdx)
				) {
				if(
					-1 == markedRangesMaskPair[0].nextSetBit(currRangeIdx) &&
						-1 == markedRangesMaskPair[1].nextSetBit(currRangeIdx)
					) {
					dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
					return;
				}
				dataIoTask.setCurrRangeIdx(currRangeIdx + 1);
				continue;
			}
			currRange = dataIoTask.getCurrRange();
			rangeSize = dataItem.getRangeSize(currRangeIdx);

			currRange.position(rangeBytesDone);
			n = (int) Math.min(chunkSize - chunkOffset, rangeSize - rangeBytesDone);
			verifyChunkData(currRange, chunkData, chunkOffset, n);
			chunkOffset += n;
			rangeBytesDone += n;

			if(rangeBytesDone == rangeSize) {
				// current byte range verification is finished
				if(
					-1 == markedRangesMaskPair[0].nextSetBit(currRangeIdx + 1) &&
						-1 == markedRangesMaskPair[1].nextSetBit(currRangeIdx + 1)
					) {
					// current byte range was last in the list
					dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
					return;
				} else {
					dataIoTask.setCurrRangeIdx(currRangeIdx + 1);
					rangeBytesDone = 0;
				}
			}
			dataIoTask.setCountBytesDone(rangeBytesDone);
		}
	}

	private static void verifyChunkUpdatedData(
		final DataIoTask dataIoTask, final DataItem item, final long countBytesDone,
		final ByteBuf chunkData, final int chunkSize
	) throws DataCorruptionException, IOException {

		int chunkCountDone = 0, remainingSize;
		long nextRangeOffset;
		int currRangeIdx;
		DataItem currRange;

		while(chunkCountDone < chunkSize) {

			currRangeIdx = dataIoTask.getCurrRangeIdx();
			nextRangeOffset = getRangeOffset(currRangeIdx + 1);
			if(countBytesDone + chunkCountDone == nextRangeOffset) {
				if(nextRangeOffset < item.size()) {
					currRangeIdx ++;
					nextRangeOffset = getRangeOffset(currRangeIdx + 1);
					dataIoTask.setCurrRangeIdx(currRangeIdx);
				} else {
					throw new DataSizeException(item.size(), countBytesDone + chunkSize);
				}
			}
			currRange = dataIoTask.getCurrRange();

			try {
				remainingSize = (int) Math.min(
					chunkSize - chunkCountDone, nextRangeOffset - countBytesDone - chunkCountDone
				);
				verifyChunkData(currRange, chunkData, chunkCountDone, remainingSize);
				chunkCountDone += remainingSize;
			} catch(final DataCorruptionException e) {
				throw new DataCorruptionException(
					getRangeOffset(dataIoTask.getCurrRangeIdx()) + e.getOffset(), e.expected,
					e.actual
				);
			}
		}
	}

	private static void verifyChunkData(
		final DataItem item, final ByteBuf chunkData, final int chunkOffset,
		final int remainingSize
	) throws DataCorruptionException, IOException {

		// fill the expected data buffer to compare with a chunk
		final ByteBuffer bb = ThreadLocalByteBuffer.get(remainingSize);
		bb.limit(remainingSize);
		int n = 0;
		while(n < remainingSize) {
			n += item.read(bb);
		}
		bb.flip();
		final ByteBuf buff = Unpooled.wrappedBuffer(bb);

		// fast compare word by word
		boolean fastEquals = true;
		int buffPos = 0;
		int chunkPos = chunkOffset;

		if(
			buff.writerIndex() - remainingSize < buffPos ||
			chunkData.writerIndex() - remainingSize < chunkPos
		) {
			fastEquals = false;
		} else {
			final int longCount = remainingSize >>> 3;
			final int byteCount = remainingSize & 7;

			// assuming the same order (big endian)
			for(int i = longCount; i > 0; i --) {
				if(buff.getLong(buffPos) != chunkData.getLong(chunkPos)) {
					fastEquals = false;
					break;
				}
				buffPos += 8;
				chunkPos += 8;
			}

			if(fastEquals) {
				for(int i = byteCount; i > 0; i --) {
					if(buff.getByte(buffPos) != chunkData.getByte(chunkPos)) {
						fastEquals = false;
						break;
					}
					buffPos ++;
					chunkPos ++;
				}
			}
		}

		if(fastEquals) {
			buff.release();
			return;
		}

		// slow byte by byte compare if fast one fails to find the exact mismatch position
		byte expected, actual;
		for(int i = 0; i < remainingSize; i ++) {
			expected = buff.getByte(i);
			actual = chunkData.getByte(chunkOffset + i);
			if(expected != actual) {
				buff.release();
				throw new DataCorruptionException(i, expected, actual);
			}
		}

		buff.release();
	}

}
