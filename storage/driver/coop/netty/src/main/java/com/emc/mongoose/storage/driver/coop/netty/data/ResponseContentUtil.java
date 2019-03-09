package com.emc.mongoose.storage.driver.coop.netty.data;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.system.DirectMemUtil;
import com.emc.mongoose.base.data.DataCorruptionException;
import com.emc.mongoose.base.data.DataSizeException;
import com.emc.mongoose.base.data.DataVerificationException;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.item.DataItem;
import static com.emc.mongoose.base.item.DataItem.rangeCount;
import static com.emc.mongoose.base.item.DataItem.rangeOffset;

import com.emc.mongoose.base.logging.Loggers;
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
					final DataOperation dataOp, final long countBytesDone, final ByteBuf contentChunk, final int chunkSize) throws IOException {
		final List<Range> byteRanges = dataOp.fixedRanges();
		final DataItem item = dataOp.item();
		try {
			if (byteRanges != null && !byteRanges.isEmpty()) {
				verifyChunkDataAndSize(dataOp, item, countBytesDone, contentChunk, chunkSize, byteRanges);
			} else if (dataOp.hasMarkedRanges()) {
				verifyChunkDataAndSize(
								dataOp, item, countBytesDone, contentChunk, chunkSize, dataOp.markedRangesMaskPair());
			} else {
				if (item.isUpdated()) {
					verifyChunkUpdatedData(dataOp, item, countBytesDone, contentChunk, chunkSize);
				} else {
					if (chunkSize > item.size() - countBytesDone) {
						throw new DataSizeException(item.size(), countBytesDone + chunkSize);
					}
					verifyChunkData(item, contentChunk, 0, chunkSize);
				}
				dataOp.countBytesDone(countBytesDone + chunkSize);
			}
		} catch (final DataVerificationException e) {
			final DataItem dataItem = dataOp.item();
			dataOp.countBytesDone(e.getOffset());
			dataOp.status(Operation.Status.RESP_FAIL_CORRUPT);
			if (e instanceof DataSizeException) {
				try {
					Loggers.MSG.debug(
									"{}: invalid size, expected: {}, actual: {} ", dataItem.name(), dataItem.size(), e.getOffset());
				} catch (final IOException ignored) {}
			} else if (e instanceof DataCorruptionException) {
				final DataCorruptionException ee = (DataCorruptionException) e;
				Loggers.MSG.debug(
								"{}: content mismatch @ offset {}, expected: {}, actual: {} ", dataItem.name(), ee.getOffset(),
								String.format("\"0x%X\"", (int) (ee.expected & 0xFF)),
								String.format("\"0x%X\"", (int) (ee.actual & 0xFF)));
			}
		}
	}

	private static void verifyChunkDataAndSize(
					final DataOperation dataOp, final DataItem dataItem, final long countBytesDone, final ByteBuf chunkData,
					final int chunkSize, final List<Range> byteRanges) throws DataCorruptionException, IOException {

		final long rangesSizeSum = dataOp.markedRangesSize();
		if (chunkSize > rangesSizeSum - countBytesDone) {
			throw new DataSizeException(dataOp.markedRangesSize(), countBytesDone + chunkSize);
		}
		final long baseItemSize = dataItem.size();

		Range byteRange;
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

		while (chunkOffset < chunkSize) {
			currRangeIdx = dataOp.currRangeIdx();
			byteRange = byteRanges.get(currRangeIdx);
			currOffset = byteRange.getBeg();
			rangeEnd = byteRange.getEnd();
			if (currOffset == -1) {
				// last "rangeEnd" bytes
				currOffset = baseItemSize - rangeEnd;
				rangeSize = rangeEnd;
			} else if (rangeEnd == -1) {
				// start @ offset equal to "currOffset"
				rangeSize = baseItemSize - currOffset;
			} else {
				rangeSize = rangeEnd - currOffset + 1;
			}

			// let (current offset = rangeBeg + rangeBytesDone)
			currOffset += rangeBytesDone;
			// find the internal data item's cell index which has:
			// (cell's offset <= current offset) && (cell's end > current offset)
			n = rangeCount(currOffset + 1) - 1;
			cellOffset = rangeOffset(n);
			cellEnd = Math.min(baseItemSize, rangeOffset(n + 1));
			// get the found cell data item (updated or not)
			currRange = dataItem.slice(cellOffset, cellEnd - cellOffset);
			if (dataItem.isRangeUpdated(n)) {
				currRange.layer(dataItem.layer() + 1);
			}
			// set the cell data item internal position to (current offset - cell's offset)
			currRange.position(currOffset - cellOffset);
			n = (int) Math.min(chunkSize - chunkOffset, Math.min(rangeSize - rangeBytesDone, cellEnd - currOffset));

			verifyChunkData(currRange, chunkData, chunkOffset, n);
			chunkOffset += n;
			rangeBytesDone += n;

			if (rangeBytesDone == rangeSize) {
				// current byte range verification is finished
				if (currRangeIdx == byteRanges.size() - 1) {
					// current byte range was last in the list
					dataOp.countBytesDone(rangesSizeSum);
					break;
				} else {
					dataOp.currRangeIdx(currRangeIdx + 1);
					rangeBytesDone = 0;
				}
			}
			dataOp.countBytesDone(rangeBytesDone);
		}
	}

	private static void verifyChunkDataAndSize(
					final DataOperation dataOp, final DataItem dataItem, final long countBytesDone, final ByteBuf chunkData,
					final int chunkSize, final BitSet markedRangesMaskPair[]) throws DataCorruptionException, IOException {

		if (chunkSize > dataOp.markedRangesSize() - countBytesDone) {
			throw new DataSizeException(dataOp.markedRangesSize(), countBytesDone + chunkSize);
		}

		DataItem currRange;
		// "countBytesDone" is the current range done bytes counter here
		long rangeBytesDone = countBytesDone;
		long rangeSize;
		int currRangeIdx;
		int chunkOffset = 0;
		int n;

		while (chunkOffset < chunkSize) {
			currRangeIdx = dataOp.currRangeIdx();
			if (!markedRangesMaskPair[0].get(currRangeIdx) && !markedRangesMaskPair[1].get(currRangeIdx)) {
				if (-1 == markedRangesMaskPair[0].nextSetBit(currRangeIdx)
								&& -1 == markedRangesMaskPair[1].nextSetBit(currRangeIdx)) {
					dataOp.countBytesDone(dataOp.markedRangesSize());
					return;
				}
				dataOp.currRangeIdx(currRangeIdx + 1);
				continue;
			}
			currRange = dataOp.currRange();
			rangeSize = dataItem.rangeSize(currRangeIdx);

			currRange.position(rangeBytesDone);
			n = (int) Math.min(chunkSize - chunkOffset, rangeSize - rangeBytesDone);
			verifyChunkData(currRange, chunkData, chunkOffset, n);
			chunkOffset += n;
			rangeBytesDone += n;

			if (rangeBytesDone == rangeSize) {
				// current byte range verification is finished
				if (-1 == markedRangesMaskPair[0].nextSetBit(currRangeIdx + 1)
								&& -1 == markedRangesMaskPair[1].nextSetBit(currRangeIdx + 1)) {
					// current byte range was last in the list
					dataOp.countBytesDone(dataOp.markedRangesSize());
					return;
				} else {
					dataOp.currRangeIdx(currRangeIdx + 1);
					rangeBytesDone = 0;
				}
			}
			dataOp.countBytesDone(rangeBytesDone);
		}
	}

	private static void verifyChunkUpdatedData(
					final DataOperation dataOp, final DataItem item, final long countBytesDone, final ByteBuf chunkData,
					final int chunkSize) throws DataCorruptionException, IOException {

		int chunkCountDone = 0, remainingSize;
		long nextRangeOffset;
		int currRangeIdx;
		DataItem currRange;

		while (chunkCountDone < chunkSize) {

			currRangeIdx = dataOp.currRangeIdx();
			nextRangeOffset = rangeOffset(currRangeIdx + 1);
			if (countBytesDone + chunkCountDone == nextRangeOffset) {
				if (nextRangeOffset < item.size()) {
					currRangeIdx++;
					nextRangeOffset = rangeOffset(currRangeIdx + 1);
					dataOp.currRangeIdx(currRangeIdx);
				} else {
					throw new DataSizeException(item.size(), countBytesDone + chunkSize);
				}
			}
			currRange = dataOp.currRange();

			try {
				remainingSize = (int) Math.min(
								chunkSize - chunkCountDone, nextRangeOffset - countBytesDone - chunkCountDone);
				verifyChunkData(currRange, chunkData, chunkCountDone, remainingSize);
				chunkCountDone += remainingSize;
			} catch (final DataCorruptionException e) {
				throw new DataCorruptionException(
								rangeOffset(dataOp.currRangeIdx()) + e.getOffset(), e.expected, e.actual);
			}
		}
	}

	private static void verifyChunkData(
					final DataItem item, final ByteBuf chunkData, final int chunkOffset, final int remainingSize) throws DataCorruptionException, IOException {

		// fill the expected data buffer to compare with a chunk
		final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(remainingSize);
		bb.limit(remainingSize);
		int n = 0;
		while (n < remainingSize) {
			n += item.read(bb);
		}
		bb.flip();
		final ByteBuf buff = Unpooled.wrappedBuffer(bb);

		// fast compare word by word
		boolean fastEquals = true;
		int buffPos = 0;
		int chunkPos = chunkOffset;

		if (buff.writerIndex() - remainingSize < buffPos || chunkData.writerIndex() - remainingSize < chunkPos) {
			fastEquals = false;
		} else {
			final int longCount = remainingSize >>> 3;
			final int byteCount = remainingSize & 7;

			// assuming the same order (big endian)
			for (int i = longCount; i > 0; i--) {
				if (buff.getLong(buffPos) != chunkData.getLong(chunkPos)) {
					fastEquals = false;
					break;
				}
				buffPos += 8;
				chunkPos += 8;
			}

			if (fastEquals) {
				for (int i = byteCount; i > 0; i--) {
					if (buff.getByte(buffPos) != chunkData.getByte(chunkPos)) {
						fastEquals = false;
						break;
					}
					buffPos++;
					chunkPos++;
				}
			}
		}

		if (fastEquals) {
			buff.release();
			return;
		}

		// slow byte by byte compare if fast one fails to find the exact mismatch position
		byte expected, actual;
		for (int i = 0; i < remainingSize; i++) {
			expected = buff.getByte(i);
			actual = chunkData.getByte(chunkOffset + i);
			if (expected != actual) {
				buff.release();
				throw new DataCorruptionException(i, expected, actual);
			}
		}

		buff.release();
	}

}
