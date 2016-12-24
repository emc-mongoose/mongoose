package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.common.io.ThreadLocalByteBuffer;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.mutable.MutableDataIoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.model.data.DataCorruptionException;
import com.emc.mongoose.model.data.DataSizeException;
import com.emc.mongoose.model.data.DataVerificationException;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_IO;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.model.item.MutableDataItem.getRangeOffset;
import com.emc.mongoose.ui.log.Markers;

import io.netty.buffer.ByteBuf;
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

/**
 Created by kurila on 04.10.16.
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
		if(!driver.isInterrupted() && !driver.isClosed()) {
			if(cause instanceof PrematureChannelClosureException) {
				LogUtil.exception(LOG, Level.WARN, cause, "Premature channel closure");
				ioTask.setStatus(FAIL_IO);
			} else if(cause instanceof DataVerificationException) {
				final DataVerificationException ee = (DataVerificationException) cause;
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				final DataItem dataItem = dataIoTask.getItem();
				dataIoTask.setCountBytesDone(ee.getOffset());
				dataIoTask.setStatus(IoTask.Status.RESP_FAIL_CORRUPT);
				if(cause instanceof DataSizeException) {
					try {
						LOG.warn(
							Markers.MSG, "{}: invalid size, expected: {}, actual: {} ",
							dataItem.getName(), dataItem.size(), ee.getOffset()
						);
					} catch(final IOException ignored) {
					}
				} else if(cause instanceof DataCorruptionException) {
					final DataCorruptionException eee = (DataCorruptionException) ee;
					LOG.warn(
						Markers.MSG, "{}: content mismatch @ offset {}, expected: {}, actual: {} ",
						dataItem.getName(), ee.getOffset(), String.format("\"0x%X\"", eee.expected),
						String.format("\"0x%X\"", eee.actual)
					);
				}
			} else {
				LogUtil.exception(LOG, Level.WARN, cause, "Client handler failure");
				ioTask.setStatus(FAIL_UNKNOWN);
			}
		}
		driver.complete(channel, ioTask);
	}

	@Override
	public final void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
	throws Exception {
		if(evt instanceof IdleStateEvent) {
			throw new SocketTimeoutException();
		}
	}
	
	protected final void verifyChunk(
		final O ioTask, final ByteBuf contentChunk, final int chunkSize
	) throws IOException {
		final DataIoTask dataIoTask = (DataIoTask) ioTask;
		final DataItem item = dataIoTask.getItem();
		final long countBytesDone = dataIoTask.getCountBytesDone();
		if(item instanceof MutableDataItem) {
			final MutableDataItem mdi = (MutableDataItem)item;
			if(mdi.isUpdated()) {
				verifyChunkUpdatedData(
					mdi, (MutableDataIoTask) ioTask, contentChunk, chunkSize
				);
				dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
			} else {
				verifyChunkDataAndSize(mdi, countBytesDone, contentChunk, chunkSize);
				dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
			}
		} else {
			verifyChunkDataAndSize(item, countBytesDone, contentChunk, chunkSize);
			dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
		}
	}

	private void verifyChunkDataAndSize(
		final DataItem item, final long countBytesDone, final ByteBuf chunkData,
		final int chunkSize
	) throws DataCorruptionException, IOException {
		if(chunkSize > item.size() - countBytesDone) {
			throw new DataSizeException(item.size(), countBytesDone + chunkSize);
		}
		verifyChunkData(item, chunkData, 0, chunkSize);
	}
	
	private void verifyChunkUpdatedData(
		final MutableDataItem item, final MutableDataIoTask ioTask, final ByteBuf chunkData,
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
					getRangeOffset(ioTask.getCurrRangeIdx()) + e.getOffset(), e.actual,
					e.expected
				);
			}
		}
	}
	
	private void verifyChunkData(
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
			return;
		}
		
		// slow byte by byte compare if fast one fails to find the exact mismatch position
		byte expected, actual;
		for(int i = 0; i < remainingSize; i++) {
			expected = buff.getByte(i);
			actual = chunkData.getByte(chunkOffset + i);
			if(expected != actual) {
				throw new DataCorruptionException(i, expected, actual);
			}
		}
	}
}
