package com.emc.mongoose.base.data.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
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
	//
	private final static String
		FMT_META_INFO = "%s" + RunTimeConfig.LIST_SEP + "%x" + LAYER_MASK_SEP + "%s",
		FMT_MSG_MASK = "Ranges mask is not correct hexadecimal value: %s",
		FMT_MSG_WRONG_RANGE_COUNT = "Range count should be more than 0 and less than the object size = %s",
		FMT_MSG_ILLEGAL_APPEND_SIZE = "Append tail size should be more than 0, but got %D",
		FMT_MASK = "0%s",
		FMT_MSG_RANGE_CORRUPT = "{}: range #{}(offset {}) of \"{}\" corrupted",
		FMT_MSG_UPD_CELL = "{}: update cell at position: {}, offset: {}, new mask: {}",
		FMT_MSG_UPD_RANGE = "{}: update range(#{}, [{}]) of with data({}, {}): {}",
		FMT_MSG_RANGE_MODIFIED = "{}: range #{} [{}-{}] was modified, layer #{}: {}",
		FMT_MSG_MERGE_MASKS = "{}: move pending ranges \"{}\" to history \"{}\"",
		STR_EMPTY_MASK = "0";
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final BitSet
		maskRangesHistory = new BitSet(),
		maskRangesPending = new BitSet();
	private volatile int layerNum = 0;
	//
	private long pendingAugmentSize = 0;
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
	}
	//
	public DataRanges(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
	}
	//
	public DataRanges(final long offset, final long size) {
		super(offset, size);
	}
	//
	public DataRanges(final long offset, final long size, final UniformDataSource dataSrc) {
		super(offset, size, dataSrc);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return String.format(
			FMT_META_INFO, super.toString(), layerNum,
			maskRangesHistory.isEmpty() ?
				STR_EMPTY_MASK : Hex.encodeHexString(maskRangesHistory.toByteArray())
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
				// extract hexadecimal layer number
				layerNum = Integer.valueOf(rangesInfo.substring(0, sepPos), 0x10);
				setDataSource(UniformDataSource.DEFAULT, layerNum);
				// extract hexadecimal mask, convert into bit set and add to the existing mask
				String rangesMask = rangesInfo.substring(sepPos + 1, rangesInfo.length());
				while(rangesMask.length() == 0 || rangesMask.length() % 2 == 1) {
					rangesMask = String.format(FMT_MASK, rangesMask);
				}
				maskRangesHistory.or(
					BitSet.valueOf(
						Hex.decodeHex(rangesMask.toCharArray())
					)
				);
			} catch(final DecoderException | NumberFormatException e) {
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
	/*public static int log2(long value) {
		int result = 0;
		if((value &  0xffffffff00000000L ) != 0	) { value >>>= 32;	result += 32; }
		if( value >= 0x10000					) { value >>>= 16;	result += 16; }
		if( value >= 0x1000						) { value >>>= 12;	result += 12; }
		if( value >= 0x100						) { value >>>= 8;	result += 8; }
		if( value >= 0x10						) { value >>>= 4;	result += 4; }
		if( value >= 0x4						) { value >>>= 2;	result += 2; }
		return result + (int) (value >>> 1);
	}*/
	//
	private static final double LOG2 = Math.log(2);
	//
	public static int getRangeCount(final long size) {
		return (int) Math.ceil(Math.log(size + 1)/LOG2);
	}
	//
	public static long getRangeOffset(final int i) {
		return (1 << i) - 1;
	}
	//
	@Override
	public final long getRangeSize(final int i) {
		return i < getRangeCount(size) - 1 ? 1 << i : size - getRangeOffset(i);
	}
	//
	@Override
	public final boolean compareWith(final InputStream in) {
		boolean contentEquals = true;
		final int countRangesTotal = getRangeCount(size);
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		for(int i = 0; i < countRangesTotal; i ++) {
			rangeOffset = getRangeOffset(i);
			rangeSize = getRangeSize(i);
			if(maskRangesHistory.get(i)) { // range have been modified
				updatedRange = new UniformData(
					offset + rangeOffset, rangeSize, layerNum + 1, UniformDataSource.DEFAULT
				);
				contentEquals = updatedRange.compareWith(in, 0, rangeSize);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					final ByteArrayOutputStream contentRangeStream = new ByteArrayOutputStream();
					updatedRange.writeTo(contentRangeStream);
					LOG.trace(
						Markers.MSG, FMT_MSG_RANGE_MODIFIED,
						Long.toHexString(offset), i, rangeOffset, rangeOffset + rangeSize - 1,
						layerNum + 1,
						Base64.encodeBase64URLSafeString(contentRangeStream.toByteArray())
					);
				}
			} else if(layerNum > 1) { // previous layer of updated ranges
				updatedRange = new UniformData(
					offset + rangeOffset, rangeSize, layerNum, UniformDataSource.DEFAULT
				);
				contentEquals = updatedRange.compareWith(in, 0, rangeSize);
			} else {
				contentEquals = compareWith(in, rangeOffset, rangeSize);
			}
			if(!contentEquals) {
				LOG.debug(
					Markers.ERR, FMT_MSG_RANGE_CORRUPT,
					Long.toHexString(offset), i, rangeOffset, toString()
				);
				break;
			}
		}
		return contentEquals;
	}
	//
	@Override
	public final boolean isRangeUpdatePending(final int i) {
		return maskRangesPending.get(i);
	}
	//
	private synchronized void switchToNextLayer() {
		layerNum ++; // increment layerNum
		maskRangesHistory.clear();
		maskRangesPending.clear(); // clear the masks
		setDataSource(UniformDataSource.DEFAULT, layerNum);
	}
	//
	@Override
	public final void updateRandomRange()
	throws IllegalStateException {
		final int
			countRangesTotal = getRangeCount(size),
			startCellPos = ThreadLocalRandom.current().nextInt(countRangesTotal);
		int nextCellPos;
		boolean updateDone = false;
		do {
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!maskRangesHistory.get(nextCellPos) && !maskRangesPending.get(nextCellPos)) {
					maskRangesPending.set(nextCellPos);
					updateDone = true;
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, FMT_MSG_UPD_CELL,
							Long.toHexString(offset), nextCellPos, getRangeOffset(nextCellPos),
							Hex.encodeHexString(maskRangesPending.toByteArray())
						);
					}
					break;
				}
			}
			if(!updateDone) { // looks like there's no free range to update left
				switchToNextLayer();
			}
		} while(!updateDone);
	}
	//
	@Override
	public final void updateRandomRanges(final int count)
	throws IllegalArgumentException, IllegalStateException {
		final int countRangesTotal = getRangeCount(size);
		if(count < 1 || count > countRangesTotal) {
			throw new IllegalArgumentException(
				String.format(
					FMT_MSG_WRONG_RANGE_COUNT, RunTimeConfig.formatSize(countRangesTotal)
				)
			);
		}
		for(int i = 0; i < count; i++) {
			updateRandomRange();
		}
	}
	//
	@Override
	public final long getPendingRangesSize() {
		final long rangeCount = getRangeCount(size);
		long pendingSize = 0;
		for(int i = 0; i < rangeCount; i ++) {
			if(maskRangesPending.get(i)) {
				pendingSize += getRangeSize(i);
			}
		}
		return pendingSize;
	}
	//
	@Override
	public final int getCountRangesTotal() {
		return getRangeCount(size);
	}
	//
	@Override
	public final void writePendingUpdatesTo(final OutputStream out)
	throws IOException {
		final int countRangesTotal = getRangeCount(size);
		DataItem nextRangeData;
		long rangeOffset, rangeSize;
		synchronized(this) {
			for(int i = 0; i < countRangesTotal; i++) {
				rangeOffset = getRangeOffset(i);
				rangeSize = getRangeSize(i);
				if(maskRangesPending.get(i)) {
					nextRangeData = new UniformData(
						offset + rangeOffset, rangeSize, layerNum + 1, UniformDataSource.DEFAULT
					);
					nextRangeData.writeTo(out);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						final ByteArrayOutputStream rangeContentStream = new ByteArrayOutputStream();
						nextRangeData.writeTo(rangeContentStream);
						LOG.trace(
							Markers.MSG, FMT_MSG_UPD_RANGE,
							toString(), i, rangeSize, offset + rangeOffset, layerNum + 1,
							Base64.encodeBase64URLSafeString(rangeContentStream.toByteArray())
						);
					}
				}
			}
			// move pending updated ranges to history
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, FMT_MSG_MERGE_MASKS,
					Long.toHexString(offset),
					Hex.encodeHexString(maskRangesPending.toByteArray()),
					Hex.encodeHexString(maskRangesHistory.toByteArray())
				);
			}
			maskRangesHistory.or(maskRangesPending);
			maskRangesPending.clear();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
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
						throw new InterruptedIOException(MSG_READ_RING_BLOCKED);
					}
				}
				if(countTailBytes > 0) { // tail bytes
					if(read(buff, 0, countTailBytes)==countTailBytes) {
						out.write(buff, 0, countTailBytes);
					} else {
						throw new InterruptedIOException(MSG_READ_RING_BLOCKED);
					}
				}
				// drop the appending on success
				pendingAugmentSize = 0;
			}
		}
	}
	//
}
