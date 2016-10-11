package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.impl.data.DataCorruptionException;
import com.emc.mongoose.model.impl.data.DataSizeException;
import com.emc.mongoose.model.util.IoWorker;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.model.api.item.MutableDataItem.getRangeOffset;

import com.emc.mongoose.ui.log.Markers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.PrematureChannelClosureException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 Created by kurila on 04.10.16.
 */
public abstract class ClientHandlerBase<M, I extends Item, O extends IoTask<I>>
extends SimpleChannelInboundHandler<M> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	protected final NetStorageDriverBase<I, O> driver;
	protected final boolean verifyFlag;
	
	protected ClientHandlerBase(final NetStorageDriverBase<I, O> driver, boolean verifyFlag) {
		this.driver = driver;
		this.verifyFlag = verifyFlag;
	}
	
	@Override
	protected final void channelRead0(final ChannelHandlerContext ctx, final M msg)
	throws Exception {
		final Channel channel = ctx.channel();
		final O ioTask = (O) channel.attr(NetStorageDriver.ATTR_KEY_IOTASK).get();
		handle(channel, ioTask, msg);
	}
	
	protected abstract void handle(final Channel channel, final O ioTask, final M msg)
	throws IOException;

	protected final void verifyChunkDataAndSize(
		final DataItem item, final long countBytesDone, final ByteBuf chunkData, final int chunkSize
	) throws DataCorruptionException, IOException {
		if(chunkSize > item.size() - countBytesDone) {
			throw new DataSizeException(item.size(), countBytesDone + chunkSize);
		}
		verifyChunkData(item, chunkData, chunkSize);
	}

	protected final void verifyChunkUpdatedData(
		final MutableDataItem item, final MutableDataIoTask ioTask, final ByteBuf chunkData,
		final int chunkSize
	) throws DataCorruptionException, IOException {

		final long countBytesDone = ioTask.getCountBytesDone();

		long verifiedByteCount = 0;
		long nextRangeOffset;
		int currRangeIdx;
		DataItem currRange;

		while(verifiedByteCount < chunkSize) {
			currRangeIdx = ioTask.getCurrRangeIdx();
			nextRangeOffset = getRangeOffset(currRangeIdx + 1);
			if(countBytesDone + verifiedByteCount == nextRangeOffset) {
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
				verifyChunkData(
					currRange, chunkData,
					(int) Math.min(chunkSize, nextRangeOffset - countBytesDone - verifiedByteCount)
				);
				verifiedByteCount += currRange.size();
			} catch(final DataCorruptionException e) {
				throw new DataCorruptionException(
					getRangeOffset(ioTask.getCurrRangeIdx()) + e.getOffset(), e.actual, e.expected
				);
			}
		}
	}

	private void verifyChunkData(
		final DataItem item, final ByteBuf chunkData, final int chunkSize
	) throws DataCorruptionException, IOException {

		// fill the expected data buffer to compare with a chunk
		final ByteBuffer bb = ((IoWorker) Thread.currentThread())
			.getThreadLocalBuff(chunkSize);
		int n = 0;
		do {
			bb.limit(chunkSize - n);
			n += item.read(bb);
		} while(n < chunkSize);
		bb.flip();
		final ByteBuf buff = Unpooled.wrappedBuffer(bb);

		// fast compare word by word
		if(0 != ByteBufUtil.compare(chunkData, buff)) {
			// slow byte by byte compare if fast one fails to find the exact mismatch position
			for(int i = 0; i < chunkSize; i ++) {
				if(buff.getByte(i) != chunkData.getByte(i)) {
					throw new DataCorruptionException(i, buff.getByte(i), chunkData.getByte(i));
				}
			}
		}
	}
	
	protected final void release(final Channel channel, final O ioTask)
	throws IOException {
		ioTask.finishResponse();
		driver.complete(channel, ioTask);
	}

	@Override @SuppressWarnings("unchecked")
	public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	throws IOException {
		if(cause instanceof PrematureChannelClosureException) {
			return;
		}
		LogUtil.exception(LOG, Level.WARN, cause, "Client handler failure");
		final Channel channel = ctx.channel();
		final O ioTask = (O) channel.attr(NetStorageDriver.ATTR_KEY_IOTASK).get();
		ioTask.setStatus(FAIL_UNKNOWN);
		driver.complete(channel, ioTask);
	}
}
