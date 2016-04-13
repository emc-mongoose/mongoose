package com.emc.mongoose.core.impl.io.task;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.ContentUtil;
import com.emc.mongoose.common.net.http.content.InputChannel;
import com.emc.mongoose.common.net.http.content.OutputChannel;

import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.data.DataCorruptionException;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.DataSizeException;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.load.executor.HttpLoadExecutor;

import com.emc.mongoose.core.impl.item.data.BasicDataItem;
import com.emc.mongoose.core.impl.item.data.BasicMutableDataItem;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;

/**
 Created by kurila on 13.04.16.
 */
public final class DataNioTaskWrapper<T extends MutableDataItem, A extends IoTask<T>>
extends NioTaskWrapper<T, A> {

	private final static Logger LOG = LogManager.getLogger();

	private final long contentSize;
	private final ContentSource contentSource;
	private final boolean verifyContentFlag;

	private volatile OutputChannel chanOut = null;
	private volatile InputChannel chanIn = null;

	private volatile long countBytesDone = 0;
	private volatile long countBytesSkipped = 0;
	private volatile DataItem currRange = null;
	private volatile long currRangeSize = 0, nextRangeOffset = 0;
	private volatile int currRangeIdx = 0, currDataLayerIdx = 0;

	public DataNioTaskWrapper(
		final A ioTask, final String nodeAddr, final HttpLoadExecutor<T, A> loadExecutor,
	    final ContentSource contentSource, final boolean verifyContentFlag
	) {
		super(ioTask, nodeAddr, loadExecutor);
		item.reset();
		this.contentSource = contentSource;
		this.verifyContentFlag = verifyContentFlag;
		currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case WRITE:
				if(item.hasScheduledUpdates()) {
					contentSize = item.getUpdatingRangesSize();
				} else if(item.isAppending()) {
					contentSize = item.getAppendSize();
				} else {
					contentSize = item.getSize();
				}
				break;
			case READ:
				// TODO partial content support
				contentSize = item.getSize();
				break;
			case DELETE:
				contentSize = 0;
				break;
			default:
				contentSize = 0;
				break;
		}
	}

	@Override
	public final void produceContent(final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {
		if(chanOut == null) { // 1st time invocation
			if(item.getSize() == 0 && item.getAppendSize() == 0) { // nothing to do
				encoder.complete();
				return;
			} else { // wrap the encoder w/ output channel
				chanOut = new OutputChannel(encoder);
				ioTask.markRequestStart();
			}
		}
		//
		try {
			switch(ioType) {
				case WRITE:
					if(item.hasScheduledUpdates()) {
						produceUpdatedRangesContent(ioctrl);
					} else if(item.isAppending()){
						produceAugmentContent(ioctrl);
					} else {
						produceObjectContent(ioctrl);
					}
					break;
				case READ:
					// TODO partial content support
					break;
				case DELETE:
					break;
			}
		} catch(final ClosedChannelException e) { // probably a manual interruption
			ioTask.setStatus(IoTask.Status.CANCELLED);
			LogUtil.exception(
				LOG, Level.TRACE, e, "#{}: output channel closed during the operation", hashCode()
			);
		} catch(final IOException e) {
			ioTask.setStatus(IoTask.Status.FAIL_IO);
			LogUtil.exception(
				LOG, Level.DEBUG, e, "#{}: I/O failure during the data output", hashCode()
			);
		} catch(final Exception e) {
			ioTask.setStatus(IoTask.Status.FAIL_UNKNOWN);
			LogUtil.exception(LOG, Level.ERROR, e, "#{}: producing content failure", hashCode());
		}
	}

	private void produceObjectContent(final IOControl ioCtl)
	throws IOException {
		countBytesDone += item.write(chanOut, contentSize - countBytesDone);
		if(countBytesDone == contentSize) {
			item.resetUpdates();
			chanOut.close();
		}
	}

	private void produceUpdatedRangesContent(final IOControl ioCtl)
	throws IOException {
		//
		if(countBytesDone + countBytesSkipped == nextRangeOffset) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "{}: written {} bytes, {} skipped, find the next updating range",
					hashCode(), countBytesDone, countBytesSkipped
				);
			}
			// find next updating range
			do {
				currRangeSize = item.getRangeSize(currRangeIdx);
				// select the current range if it's updating
				if(item.isCurrLayerRangeUpdating(currRangeIdx)) {
					currRange = new BasicDataItem(
						item.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 1,
						contentSource
					);
				} else if(item.isNextLayerRangeUpdating(currRangeIdx)) {
					currRange = new BasicDataItem(
						item.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 2,
						contentSource
					);
				} else {
					countBytesSkipped += currRangeSize;
					currRange = null;
				}
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "{}: range #{} is updating? - {}, written {} bytes, {} skipped",
						hashCode(), currRangeIdx, currRange != null, countBytesDone,
						countBytesSkipped
					);
				}
				currRangeIdx ++;
				nextRangeOffset = BasicMutableDataItem.getRangeOffset(currRangeIdx);
			} while(currRange == null && currRangeSize > 0 && countBytesDone < contentSize);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "{}: next updating range found: #{}, size: {}",
					hashCode(), currRangeIdx - 1, currRangeSize
				);
			}
		}
		// write the current updating range's content
		if(currRangeSize > 0 && currRange != null) {
			countBytesDone += currRange.write(
				chanOut, nextRangeOffset - countBytesDone - countBytesSkipped
			);
		}
		if(countBytesDone == contentSize) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "{}: finish the updating, {} bytes done, {} skipped",
					hashCode(), countBytesDone, countBytesSkipped
				);
			}
			item.commitUpdatedRanges();
			chanOut.close();
		}
	}

	private void produceAugmentContent(final IOControl ioCtl)
	throws IOException {
		if(currRange == null) {
			final long prevSize = item.getSize();
			currRangeIdx = prevSize > 0 ? BasicMutableDataItem.getRangeCount(prevSize) - 1 : 0;
			if(item.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
					item.getOffset() + prevSize, contentSize, currDataLayerIdx + 1, contentSource
				);
			} else {
				currRange = new BasicDataItem(
					item.getOffset() + prevSize, contentSize, currDataLayerIdx, contentSource
				);
			}
		}
		//
		countBytesDone += item.write(chanOut, contentSize - countBytesDone);
		if(countBytesDone == contentSize) {
			item.commitAppend();
			chanOut.close();
		}
	}

	@Override
	public final void consumeContent(final ContentDecoder decoder, final IOControl ioctrl) {
		if(chanIn == null) {
			chanIn = new InputChannel(decoder);
			ioTask.markResponseDataStart();
		}
		try {
			if(IoTask.Status.SUCC.equals(ioTask.getStatus())) { // failure, no user data is expected
				// check for the content corruption
				if(LoadType.READ.equals(ioType)) {
					// just consume quietly if marked as corrupted once
					if(
						verifyContentFlag
							&&
						!IoTask.Status.RESP_FAIL_CORRUPT.equals(ioTask.getStatus())
					) {
						// should verify the content
						consumeAndVerifyContent(decoder, ioctrl);
					} else { // consume quietly
						countBytesDone += ContentUtil.consumeQuietly(
							decoder, contentSize - countBytesDone
						);
					}
				} else {
					super.consumeContent(decoder, ioctrl);
				}
			} else {
				consumeFailedResponseContent(decoder, ioctrl);
			}
		} catch(final ClosedChannelException e) {
			ioTask.setStatus(IoTask.Status.CANCELLED);
			LogUtil.exception(LOG, Level.TRACE, e, "Output channel closed during the operation");
		} catch(final IOException e) {
			if(!loadExecutor.isClosed()) {
				LogUtil.exception(LOG, Level.DEBUG, e, "I/O failure during content consuming");
			}
		}
	}

	private void consumeFailedResponseContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		final ByteBuffer bbuff = ByteBuffer.allocate(Constants.BUFF_SIZE_LO);
		while(in.read(bbuff) >= 0 && bbuff.remaining() > 0);
		LOG.debug(
			Markers.ERR, "#{}: {} - {}", hashCode(), ioTask.getStatus().description,
			new String(bbuff.array(), 0, bbuff.position(), StandardCharsets.UTF_8)
		);
		chanIn.close();
	}

	private void consumeAndVerifyContent(final ContentDecoder decoder, final IOControl ioCtl)
	throws IOException {
		final ByteBuffer buffIn;
		try {
			if(item.hasBeenUpdated()) {
				// switch the range if current is done or not set yet
				if(countBytesDone == nextRangeOffset) {
					currRangeSize = item.getRangeSize(currRangeIdx);
					if(item.isCurrLayerRangeUpdated(currRangeIdx)) {
						currRange = new BasicDataItem(
							item.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 1,
							contentSource
						);
					} else {
						currRange = new BasicDataItem(
							item.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx,
							contentSource
						);
					}
					currRangeIdx ++;
					nextRangeOffset = BasicMutableDataItem.getRangeOffset(currRangeIdx);
				}
				//
				if(currRangeSize > 0) {
					buffIn = ((IOWorker) Thread.currentThread())
						.getThreadLocalBuff(nextRangeOffset - countBytesDone);
					final int n = currRange.readAndVerify(chanIn, buffIn);
					if(n > 0) {
						countBytesDone += n;
					}
					if(countBytesDone == contentSize) {
						chanIn.close();
					}
				} else {
					chanIn.close();
				}
			} else {
				buffIn = ((IOWorker) Thread.currentThread())
					.getThreadLocalBuff(contentSize - countBytesDone);
				final int n = item.readAndVerify(chanIn, buffIn);
				if(n > 0) {
					countBytesDone += n;
				}
			}
		} catch(final DataSizeException e) {
			countBytesDone += e.offset;
			LOG.warn(
				Markers.MSG,
				"{}: content size mismatch, expected: {}, actual: {}",
				item.getName(), item.getSize(), e.offset
			);
			ioTask.setStatus(IoTask.Status.RESP_FAIL_CORRUPT);
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
			ioTask.setStatus(IoTask.Status.RESP_FAIL_CORRUPT);
		}
	}

	@Override
	public final void responseCompleted(final HttpContext context) {
		ioTask.markResponseDone(countBytesDone);
	}
}
