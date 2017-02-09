package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.io.ThreadLocalByteBuffer;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.data.DataCorruptionException;
import com.emc.mongoose.model.data.DataSizeException;
import com.emc.mongoose.model.data.DataVerificationException;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.model.io.task.IoTask.Status.CANCELLED;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_IO;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.model.item.DataItem.getRangeOffset;
import com.emc.mongoose.ui.log.Markers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.timeout.IdleStateEvent;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 Created by kurila on 04.10.16.
 Contains the content validation functionality
 */
public abstract class ResponseHandlerBase<
	M, I extends Item, O extends IoTask<I, R>, R extends IoResult
>
extends SimpleChannelInboundHandler<M> {
	
	private static final Logger LOG = LogManager.getLogger();

	protected final NetStorageDriverBase<I, O, R> driver;
	protected final boolean verifyFlag;
	
	protected ResponseHandlerBase(final NetStorageDriverBase<I, O, R> driver, boolean verifyFlag) {
		this.driver = driver;
		this.verifyFlag = verifyFlag;
	}
	
	@Override @SuppressWarnings("unchecked")
	protected final void channelRead0(final ChannelHandlerContext ctx, final M msg)
	throws Exception {
		final Channel channel = ctx.channel();
		final O ioTask = (O) channel.attr(NetStorageDriver.ATTR_KEY_IOTASK).get();
		handle(channel, ioTask, msg);
	}
	
	protected abstract void handle(final Channel channel, final O ioTask, final M msg)
	throws IOException;

	@Override @SuppressWarnings("unchecked")
	public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	throws IOException {
		final Channel channel = ctx.channel();
		final O ioTask = (O) channel.attr(NetStorageDriver.ATTR_KEY_IOTASK).get();
		if(driver.isInterrupted() || driver.isClosed()) {
			ioTask.setStatus(CANCELLED);
		} else if(!driver.isInterrupted() && !driver.isClosed()) {
			if(cause instanceof PrematureChannelClosureException) {
				LogUtil.exception(LOG, Level.WARN, cause, "Premature channel closure");
				ioTask.setStatus(FAIL_IO);
			} else {
				LogUtil.exception(LOG, Level.WARN, cause, "Client handler failure");
				ioTask.setStatus(FAIL_UNKNOWN);
			}
		}
		if(!driver.isClosed()) {
			try {
				driver.complete(channel, ioTask);
			} catch(final RejectedExecutionException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to complete the I/O task");
			}
		}
	}

	@Override
	public final void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
	throws Exception {
		if(evt instanceof IdleStateEvent) {
			throw new SocketTimeoutException();
		}
	}
	
	protected static void verifyChunk(
		final DataIoTask dataIoTask, final ByteBuf contentChunk, final int chunkSize
	) throws IOException {

		final long countBytesDone = dataIoTask.getCountBytesDone();
		final List<ByteRange> byteRanges = dataIoTask.getFixedRanges();

		final DataItem item = dataIoTask.getItem();
		try {
			if(item.isUpdated()) {
				if(byteRanges != null && !byteRanges.isEmpty()) {
					verifyChunkUpdatedData(dataIoTask, contentChunk, chunkSize, byteRanges);
				} else if(dataIoTask.hasMarkedRanges()) {
					verifyChunkUpdatedData(
						dataIoTask, contentChunk, chunkSize, dataIoTask.getMarkedRangesMaskPair()
					);
				} else {
					verifyChunkUpdatedData(item, dataIoTask, contentChunk, chunkSize);
				}
				dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
			} else {
				if(byteRanges != null && !byteRanges.isEmpty()) {
					verifyChunkDataAndSize(
						dataIoTask, countBytesDone, contentChunk, chunkSize, byteRanges
					);
				} else if(dataIoTask.hasMarkedRanges()) {
					verifyChunkDataAndSize(
						dataIoTask, countBytesDone, contentChunk, chunkSize,
						dataIoTask.getMarkedRangesMaskPair()
					);
				} else {
					verifyChunkDataAndSize(item, countBytesDone, contentChunk, chunkSize);
				}
				dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
			}
		} catch(final DataVerificationException e) {
			final DataItem dataItem = dataIoTask.getItem();
			dataIoTask.setCountBytesDone(e.getOffset());
			dataIoTask.setStatus(IoTask.Status.RESP_FAIL_CORRUPT);
			if(e instanceof DataSizeException) {
				try {
					LOG.warn(
						Markers.MSG, "{}: invalid size, expected: {}, actual: {} ",
						dataItem.getName(), dataItem.size(), e.getOffset()
					);
				} catch(final IOException ignored) {
				}
			} else if(e instanceof DataCorruptionException) {
				final DataCorruptionException ee = (DataCorruptionException) e;
				LOG.warn(
					Markers.MSG, "{}: content mismatch @ offset {}, expected: {}, actual: {} ",
					dataItem.getName(), ee.getOffset(), String.format("\"0x%X\"", ee.expected),
					String.format("\"0x%X\"", ee.actual)
				);
			}
		}
	}

	private static void verifyChunkDataAndSize(
		final DataItem item, final long countBytesDone, final ByteBuf chunkData,
		final int chunkSize
	) throws DataCorruptionException, IOException {
		if(chunkSize > item.size() - countBytesDone) {
			throw new DataSizeException(item.size(), countBytesDone + chunkSize);
		}
		verifyChunkData(item, chunkData, 0, chunkSize);
	}

	private static void verifyChunkDataAndSize(
		final DataIoTask dataIoTask, final long countBytesDone, final ByteBuf chunkData,
		final int chunkSize, final List<ByteRange> byteRanges
	) throws DataCorruptionException, IOException {
		
		final long rangesSizeSum = dataIoTask.getMarkedRangesSize();
		if(chunkSize > rangesSizeSum - countBytesDone) {
			throw new DataSizeException(
				dataIoTask.getMarkedRangesSize(), countBytesDone + chunkSize
			);
		}
		// "countBytesDone" is the current range done bytes counter here
		final DataItem dataItem = dataIoTask.getItem();
		final long baseItemSize = dataItem.size();
		
		ByteRange byteRange;
		DataItem currRange;
		long rangeBytesDone = countBytesDone;
		long rangeBeg;
		long rangeEnd;
		long rangeSize;
		int currRangeIdx;
		int chunkOffset = 0;
		int n;
		
		while(chunkOffset < chunkSize) {
			currRangeIdx = dataIoTask.getCurrRangeIdx();
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
				currRange = dataItem.slice(rangeBeg, rangeSize);
				currRange.position(rangeBytesDone);
				n = (int) Math.min(chunkSize - chunkOffset, rangeSize - rangeBytesDone);
				verifyChunkData(currRange, chunkData, chunkOffset, n);
				chunkOffset += n;
				rangeBytesDone += n;
				
				if(rangeBytesDone == rangeSize) {
					dataIoTask.setCurrRangeIdx(currRangeIdx + 1);
					rangeBytesDone = 0;
				}
				dataIoTask.setCountBytesDone(rangeBytesDone);
			} else {
				dataIoTask.setCountBytesDone(rangesSizeSum);
				break;
			}
		}
	}

	private static void verifyChunkDataAndSize(
		final DataIoTask dataIoTask, final long countBytesDone, final ByteBuf chunkData,
		final int chunkSize, final BitSet markedRangesMaskPair[]
	) throws DataCorruptionException, IOException {
		if(chunkSize > dataIoTask.getMarkedRangesSize() - countBytesDone) {
			throw new DataSizeException(
				dataIoTask.getMarkedRangesSize(), countBytesDone + chunkSize
			);
		}
		// TODO
		// "countBytesDone" is the current range done bytes counter here
	}
	
	private static void verifyChunkUpdatedData(
		final DataItem item, final DataIoTask ioTask, final ByteBuf chunkData,
		final int chunkSize
	) throws DataCorruptionException, IOException {
		
		final long countBytesDone = ioTask.getCountBytesDone();
		int chunkCountDone = 0, remainingSize;
		long nextRangeOffset;
		int currRangeIdx;
		DataItem currRange;
		
		while(chunkCountDone < chunkSize) {
			
			currRangeIdx = ioTask.getCurrRangeIdx();
			nextRangeOffset = getRangeOffset(currRangeIdx + 1);
			if(countBytesDone + chunkCountDone == nextRangeOffset) {
				if(nextRangeOffset < item.size()) {
					currRangeIdx ++;
					nextRangeOffset = getRangeOffset(currRangeIdx + 1);
					ioTask.setCurrRangeIdx(currRangeIdx);
				} else {
					throw new DataSizeException(item.size(), countBytesDone + chunkSize);
				}
			}
			currRange = ioTask.getCurrRange();
			
			try {
				remainingSize = (int) Math.min(
					chunkSize - chunkCountDone, nextRangeOffset - countBytesDone - chunkCountDone
				);
				verifyChunkData(currRange, chunkData, chunkCountDone, remainingSize);
				chunkCountDone += remainingSize;
			} catch(final DataCorruptionException e) {
				throw new DataCorruptionException(
					getRangeOffset(ioTask.getCurrRangeIdx()) + e.getOffset(), e.expected,
					e.actual
				);
			}
		}
	}

	private static void verifyChunkUpdatedData(
		final DataIoTask ioTask, final ByteBuf chunkData, final int chunkSize,
		final List<ByteRange> byteRanges
	) throws DataCorruptionException, IOException {

	}

	private static void verifyChunkUpdatedData(
		final DataIoTask ioTask, final ByteBuf chunkData, final int chunkSize,
		final BitSet markedRangesMaskPair[]
	) throws DataCorruptionException, IOException {

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
		for(int i = 0; i < remainingSize; i++) {
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
