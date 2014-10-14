package com.emc.mongoose.base.data.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 15.09.14.
 A uniform data extension which may be logically split into isolated ranges for appends and updates.
 */
public class DataRanges
extends UniformData
implements AppendableDataItem, UpdatableDataItem {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static char LAYER_MASK_SEP = '/';
	private final static String
		FMT_META_INFO = "%s" + RunTimeConfig.LIST_SEP + "%x" + LAYER_MASK_SEP + "%s",
		FMT_MSG_MASK = "Ranges mask is not correct hexadecimal value: %s",
		FMT_MSG_WRONG_RANGE_COUNT = "Range count should be more than 0 and less than the object size = %s";
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final BitSet
		maskRangesHistory = new BitSet(),
		maskRangesPending = new BitSet();
	private int countRangesTotal, rangeSize;
	private volatile int layerNum = 0;
	//
	private long pendingAugmentSize = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private static int calcTotalCount(long size)
		throws IllegalArgumentException {
		if(size > 0) {
			return Long.SIZE - Long.numberOfLeadingZeros(size) - 1 + (int) Math.sqrt(Math.sqrt(size));
		} else {
			throw new IllegalArgumentException(
				String.format("The size should be more than zero, but got %d", size)
			);
		}
	}
	//
	private void initRanges(final int layerNum) {
		countRangesTotal = calcTotalCount(size);
		rangeSize = (int) (size / countRangesTotal);
		this.layerNum = layerNum;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public DataRanges() {
		super(); // ranges remain uninitialized
	}
	//
	public DataRanges(final String metaInfo) {
		fromString(metaInfo); // invokes ranges initialization
	}
	//
	public DataRanges(final long size) {
		super(size);
		initRanges(1);
	}
	//
	public DataRanges(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
		initRanges(1);
	}
	//
	public DataRanges(final long offset, final long size) {
		super(offset, size);
		initRanges(1);
	}
	//
	public DataRanges(final long offset, final long size, final UniformDataSource dataSrc) {
		super(offset, size, dataSrc);
		initRanges(1);
	}
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return String.format(
			FMT_META_INFO, super.toString(),
			layerNum, Hex.encodeHexString(maskRangesHistory.toByteArray())
		);
	}
	//
	@Override
	public void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		final int lastCommaPos = v.lastIndexOf(RunTimeConfig.LIST_SEP);
		final String baseItemInfo, rangesInfo;
		if(lastCommaPos > 0) {
			baseItemInfo = v.substring(0, lastCommaPos);
			super.fromString(baseItemInfo);
			rangesInfo = v.substring(lastCommaPos + 1, v.length());
			final int sepPos = rangesInfo.indexOf(LAYER_MASK_SEP);
			try {
				initRanges(
					Integer.valueOf(
						rangesInfo.substring(0, sepPos),
						0x10
					)
				);
				maskRangesHistory.or(
					BitSet.valueOf(
						Hex.decodeHex(
							rangesInfo.substring(sepPos + 1, rangesInfo.length()).toCharArray()
						)
					)
				);
			} catch(final DecoderException|NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_MASK, rangesInfo));
			}
		} else {
			throw new IllegalArgumentException(String.format(FMT_MSG_INVALID_RECORD, v));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		return super.hashCode() ^ maskRangesHistory.hashCode() ^ maskRangesPending.hashCode();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(layerNum);
		out.writeObject(maskRangesHistory);
		out.writeObject(maskRangesPending);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		layerNum = in.readInt();
		maskRangesHistory.or(BitSet.class.cast(in.readObject()));
		maskRangesPending.or(BitSet.class.cast(in.readObject()));
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean compareWith(final InputStream in) {
		boolean contentEquals = true;
		final int tailSize = (int) (size - countRangesTotal * rangeSize);
		long rangeOffset;
		UniformData updatedRange;
		for(int i=0; i<countRangesTotal; i++) {
			rangeOffset = i * rangeSize;
			if(maskRangesHistory.get(i)) { // range have been modified
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Range #{} [{}-{}] was modified",
						i, rangeOffset, rangeOffset + rangeSize - 1
					);
				}
				updatedRange = new UniformData(
					offset + rangeOffset, rangeSize, layerNum, UniformDataSource.DEFAULT
				);
				contentEquals = updatedRange.compareWith(in, 0, rangeSize);
			} else if(layerNum > 1) { // previous layer of updated ranges
				updatedRange = new UniformData(
					offset + rangeOffset, rangeSize, layerNum - 1, UniformDataSource.DEFAULT
				);
				contentEquals = updatedRange.compareWith(in, 0, rangeSize);
			} else { // pristine object content
				contentEquals = compareWith(in, rangeOffset, rangeSize);
			}
			if(!contentEquals) {
				if(LOG.isTraceEnabled()) {
					LOG.trace(
						Markers.MSG, "Range #{}(offset {}) corrupted?", i, rangeOffset
					);
				}
				break;
			}
		}
		if(contentEquals && tailSize > 0) {
			contentEquals = compareWith(in, countRangesTotal * rangeSize, tailSize);
		}
		return contentEquals;
	}
	//
	@Override
	public final boolean isRangeUpdatePending(final int i) {
		return maskRangesPending.get(i);
	}
	//
	@Override
	public final void updateRandomRange()
		throws IllegalStateException {
		final int startCellPos = ThreadLocalRandom.current().nextInt(countRangesTotal);
		int nextCellPos;
		for(int i=startCellPos; i<startCellPos+countRangesTotal; i++) {
			nextCellPos = i % countRangesTotal;
			if(!maskRangesHistory.get(nextCellPos) && !maskRangesPending.get(nextCellPos)) {
				maskRangesPending.set(nextCellPos);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Update cell at position: {}, offset: {}, new mask: {}",
						nextCellPos, nextCellPos * rangeSize,
						Hex.encodeHexString(maskRangesPending.toByteArray())
					);
				}
				return;
			}
		}
		// looks like there's no free range to update left
		synchronized(this) {
			layerNum ++; // increment layerNum
			maskRangesHistory.clear(); maskRangesPending.clear(); // clear the masks
			updateRandomRange(); // try again
		}
	}
	//
	@Override
	public final void updateRandomRanges(final int count)
	throws IllegalArgumentException, IllegalStateException {
		if(count < 1 || count > countRangesTotal) {
			throw new IllegalArgumentException(
				String.format(FMT_MSG_WRONG_RANGE_COUNT, RunTimeConfig.formatSize(countRangesTotal))
			);
		}
		for(int i = 0; i < count; i++) {
			updateRandomRange();
		}
	}
	//
	@Override
	public final int getPendingUpdatesCount() {
		int count = 0;
		for(int i=0; i<countRangesTotal; i++) {
			if(maskRangesPending.get(i)) {
				count ++;
			}
		}
		return count;
	}
	/*
	public final InputStream getPendingUpdatesContent() {
		InputStream updatesContent = null;
		UniformData nextRangeData;
		long rangeOffset;
		for(int i=0; i<countRangesTotal; i++) {
			if(maskRangesPending.get(i)) {
				rangeOffset = i * rangeSize;
				LOG.trace(
					Markers.MSG, "Append range with offset {} and size {} to content for update",
					offset + rangeOffset, rangeSize
				);
				nextRangeData = new UniformData(
					offset + rangeOffset, rangeSize, UniformDataSource.DATA_SRC_UPDATE
				);
				if(updatesContent==null) {
					updatesContent = nextRangeData;
				} else {
					updatesContent = new SequenceInputStream(updatesContent, nextRangeData);
				}
			}
		}
		return updatesContent;
	}*/
	//
	@Override
	public final int getCountRangesTotal() {
		return countRangesTotal;
	}
	//
	@Override
	public final int getRangeSize() {
		return rangeSize;
	}
	//
	@Override
	public final void writePendingUpdatesTo(final OutputStream out)
	throws IOException {
		DataItem nextRangeData;
		long rangeOffset;
		synchronized(this) {
			for(int i = 0; i < countRangesTotal; i++) {
				rangeOffset = i * rangeSize;
				if(maskRangesPending.get(i)) {
					nextRangeData = new UniformData(
						offset + rangeOffset, rangeSize, layerNum, UniformDataSource.DEFAULT
					);
					nextRangeData.writeTo(out);
				}
			}
			// move pending updated ranges to history
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Move pending ranges \"{}\" to history \"{}\"",
					Hex.encodeHexString(maskRangesPending.toByteArray()),
					Hex.encodeHexString(maskRangesHistory.toByteArray())
				);
			}
			maskRangesHistory.or(maskRangesPending);
			maskRangesPending.clear();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static String
		FMT_MSG_ILLEGAL_APPEND_SIZE = "Append tail size should be more than 0, but got %D";
	//
	@Override
	public final void append(final long augmentSize) {
		if(augmentSize > 0) {
			pendingAugmentSize = augmentSize;
		} else {
			throw new IllegalArgumentException(
				String.format(FMT_MSG_ILLEGAL_APPEND_SIZE, augmentSize)
			);
		}
	}
	//
	@Override
	public final long getPendingAugmentSize() {
		return pendingAugmentSize;
	}
	//
	@Override
	public final void writeAugmentTo(final OutputStream out)
	throws IOException {
		if(pendingAugmentSize > 0) {
			synchronized(this) {
				setOffset(offset, size);
				// change the size
				size += pendingAugmentSize;
				// redirect the tail's data to the output
				final byte buff[] = new byte[
					pendingAugmentSize < MAX_PAGE_SIZE ? (int) pendingAugmentSize : MAX_PAGE_SIZE
				];
				final int
					countPages = (int) pendingAugmentSize / buff.length,
					countTailBytes = (int) pendingAugmentSize % buff.length;
				//
				for(int i = 0; i < countPages; i++) {
					if(read(buff)==buff.length) {
						out.write(buff);
					} else {
						throw new InterruptedIOException("Reading from data ring blocked?");
					}
				}
				if(countTailBytes > 0) { // tail bytes
					if(read(buff, 0, countTailBytes)==countTailBytes) {
						out.write(buff, 0, countTailBytes);
					} else {
						throw new InterruptedIOException("Reading from data ring blocked?");
					}
				}
				// drop the appending on success
				pendingAugmentSize = 0;
			}
		}
	}
	//
}
