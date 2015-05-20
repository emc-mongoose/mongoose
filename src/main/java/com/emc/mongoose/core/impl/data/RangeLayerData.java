package com.emc.mongoose.core.impl.data;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.AppendableDataItem;
import com.emc.mongoose.core.api.data.UpdatableDataItem;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
//
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
import java.io.SequenceInputStream;
import java.util.BitSet;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 15.09.14.
 A uniform data extension which may be logically split into isolated ranges for appends and updates.
 */
public class RangeLayerData
extends UniformData
implements AppendableDataItem, UpdatableDataItem {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static char LAYER_MASK_SEP = '/';
	//
	protected final static String
		FMT_MSG_MASK = "Ranges mask is not correct hexadecimal value: %s",
		FMT_MSG_RANGE_CORRUPT = "{}: range #{}(offset {}) of \"{}\" corrupted",
		FMT_MSG_UPD_CELL = "{}: update cell at position: {}, offset: {}, new mask: {}",
		FMT_MSG_UPD_RANGE = "{}: update range(#{}, [{}]) of with data({}, {}): {}",
		FMT_MSG_RANGE_MODIFIED = "{}: range #{} [{}-{}] was modified, layer #{}: {}",
		FMT_MSG_MERGE_MASKS = "{}: move pending ranges \"{}\" to history \"{}\"",
		STR_EMPTY_MASK = "0";
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final BitSet maskRangesHistory = new BitSet(), maskRangesPending = new BitSet();
	protected AtomicInteger currLayerIndex = new AtomicInteger();
	//
	protected long pendingAugmentSize = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public RangeLayerData() {
		super(); // ranges remain uninitialized
	}
	//
	public RangeLayerData(final String metaInfo) {
		fromString(metaInfo); // invokes ranges initialization
	}
	//
	public RangeLayerData(final Long size) {
		super(size);
	}
	//
	public RangeLayerData(final Long offset, final Long size) {
		super(offset, size);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return super.toString() + ',' + Integer.toHexString(currLayerIndex.get()) + '/' +
			(
				maskRangesHistory.isEmpty() ?
					STR_EMPTY_MASK : Hex.encodeHexString(maskRangesHistory.toByteArray())
			)
		;
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
				currLayerIndex.set(Integer.valueOf(rangesInfo.substring(0, sepPos), 0x10));
				setDataSource(UniformDataSource.DEFAULT, currLayerIndex.get());
				// extract hexadecimal mask, convert into bit set and add to the existing mask
				final String rangesMask = rangesInfo.substring(sepPos + 1, rangesInfo.length());
				final char rangesMaskChars[];
				if(rangesMask.length() == 0) {
					rangesMaskChars = ("00" + rangesMask).toCharArray();
				} else if(rangesMask.length() % 2 == 1) {
					rangesMaskChars = ("0" + rangesMask).toCharArray();
				} else {
					rangesMaskChars = rangesMask.toCharArray();
				}
				// method "or" to merge w/ the existing mask
				maskRangesHistory.or(
					BitSet.valueOf(Hex.decodeHex(rangesMaskChars))
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
		out.writeInt(currLayerIndex.get());
		out.writeObject(maskRangesHistory);
		out.writeObject(maskRangesPending);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		currLayerIndex.set(in.readInt());
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
	@Override
	public final int getCurrLayerIndex() {
		return currLayerIndex.get();
	}
	//
	private static final double LOG2 = Math.log(2);
	//
	public static int getRangeCount(final long size) {
		return (int) Math.ceil(Math.log(size + 1) / LOG2);
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
	public final int getCountRangesTotal() {
		return getRangeCount(size);
	}
	//
	@Override
	public final boolean isContentEqualTo(final InputStream in)
	throws IOException {
		// do not go over ranges if there's no updated ones
		if(maskRangesHistory.isEmpty()) {
			if(currLayerIndex.get() == 0) {
				return isContentEqualTo(in, 0, size);
			} else {
				return new UniformData(
					offset, size, currLayerIndex.get(), UniformDataSource.DEFAULT
				).isContentEqualTo(in, 0, size);
			}
		}
		//
		boolean contentEquals = true;
		final int countRangesTotal = size > 0 ? getRangeCount(size) : Integer.MAX_VALUE;
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		for(int i = 0; i < countRangesTotal; i ++) {
			rangeOffset = getRangeOffset(i);
			rangeSize = getRangeSize(i);
			if(maskRangesHistory.get(i)) {
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "{}: range #{} has been modified", Long.toHexString(offset), i
					);
				}
				updatedRange = new UniformData(
					offset + rangeOffset, rangeSize, currLayerIndex.get() + 1, UniformDataSource.DEFAULT
				);
				contentEquals = updatedRange.isContentEqualTo(in, 0, rangeSize);
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					final ByteArrayOutputStream contentRangeStream = new ByteArrayOutputStream();
					updatedRange.writeTo(contentRangeStream);
					LOG.trace(
						LogUtil.MSG, FMT_MSG_RANGE_MODIFIED,
						Long.toHexString(offset), i, rangeOffset, rangeOffset + rangeSize - 1,
						currLayerIndex.get() + 1,
						Base64.encodeBase64URLSafeString(contentRangeStream.toByteArray())
					);
				}
			} else if(currLayerIndex.get() > 1) {
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "{}: range #{} contains previous layer of data",
						Long.toHexString(offset), i
					);
				}
				updatedRange = new UniformData(
					offset + rangeOffset, rangeSize, currLayerIndex.get(), UniformDataSource.DEFAULT
				);
				contentEquals = updatedRange.isContentEqualTo(in, 0, rangeSize);
			} else {
				contentEquals = isContentEqualTo(in, rangeOffset, rangeSize);
			}
			if(!contentEquals) {
				LOG.debug(
					LogUtil.ERR, FMT_MSG_RANGE_CORRUPT,
					Long.toHexString(offset), i, rangeOffset, toString()
				);
				break;
			}
		}
		return contentEquals;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// UPDATE //////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean isRangeUpdatePending(final int i) {
		return maskRangesPending.get(i);
	}
	//
	protected synchronized void switchToNextOverlay() {
		maskRangesHistory.clear();
		maskRangesPending.clear(); // clear the masks
		setDataSource(UniformDataSource.DEFAULT, currLayerIndex.incrementAndGet()); // increment layer index
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
					if(LOG.isTraceEnabled(LogUtil.MSG)) {
						LOG.trace(
							LogUtil.MSG, FMT_MSG_UPD_CELL,
							Long.toHexString(offset), nextCellPos, getRangeOffset(nextCellPos),
							Hex.encodeHexString(maskRangesPending.toByteArray())
						);
					}
					break;
				}
			}
			if(!updateDone) { // looks like there's no free range to update left
				switchToNextOverlay();
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
				"Range count should be more than 0 and less than max " + countRangesTotal +
				" for the item size"
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
						offset + rangeOffset, rangeSize, currLayerIndex.get() + 1, UniformDataSource.DEFAULT
					);
					nextRangeData.writeTo(out);
					if(LOG.isTraceEnabled(LogUtil.MSG)) {
						final ByteArrayOutputStream rangeContentStream = new ByteArrayOutputStream();
						nextRangeData.writeTo(rangeContentStream);
						LOG.trace(
							LogUtil.MSG, FMT_MSG_UPD_RANGE,
							toString(), i, rangeSize, offset + rangeOffset, currLayerIndex.get() + 1,
							Base64.encodeBase64URLSafeString(rangeContentStream.toByteArray())
						);
					}
				}
			}
			// move pending updated ranges to history
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(
					LogUtil.MSG, FMT_MSG_MERGE_MASKS,
					Long.toHexString(offset),
					Hex.encodeHexString(maskRangesPending.toByteArray()),
					Hex.encodeHexString(maskRangesHistory.toByteArray())
				);
			}
			maskRangesHistory.or(maskRangesPending);
			maskRangesPending.clear();
		}
	}
	//
	@Override
	public final InputStream getPendingUpdatesContent() {
		final int countRangesTotal = getRangeCount(size);
		final Vector<InputStream> updatedRangesData = new Vector<>();
		long rangeOffset, rangeSize;
		synchronized(this) {
			for(int i = 0; i < countRangesTotal; i++) {
				rangeOffset = getRangeOffset(i);
				rangeSize = getRangeSize(i);
				if(maskRangesPending.get(i)) {
					updatedRangesData.add(
						new UniformData(
							offset + rangeOffset, rangeSize, currLayerIndex.get() + 1, UniformDataSource.DEFAULT
						)
					);
				}
			}
			// move pending updated ranges to history
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(
					LogUtil.MSG, FMT_MSG_MERGE_MASKS,
					Long.toHexString(offset),
					Hex.encodeHexString(maskRangesPending.toByteArray()),
					Hex.encodeHexString(maskRangesHistory.toByteArray())
				);
			}
			maskRangesHistory.or(maskRangesPending);
			maskRangesPending.clear();
		}
		return new SequenceInputStream(updatedRangesData.elements());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// APPEND //////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void append(final long augmentSize)
	throws IllegalArgumentException {
		if(augmentSize > 0) {
			pendingAugmentSize = augmentSize;
			final int
				lastCellPos = size > 0 ? getRangeCount(size) - 1 : 0,
				nextCellPos = getRangeCount(size + augmentSize);
			if(lastCellPos < nextCellPos && maskRangesHistory.get(lastCellPos)) {
				maskRangesPending.set(lastCellPos, nextCellPos);
			}
		} else {
			throw new IllegalArgumentException(
				"Append tail size should be more than 0, but got " + augmentSize
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
				final int rangeIndex = size > 0 ? getRangeCount(size) - 1 : 0;
				if(maskRangesHistory.get(rangeIndex)) { // write from next layer
					new UniformData(
						offset + size, pendingAugmentSize, currLayerIndex.get() + 1,
						UniformDataSource.DEFAULT
					).writeTo(out);
					size += pendingAugmentSize;
				} else { // write from current layer
					setRelativeOffset(size);
					// change the size
					size += pendingAugmentSize;
					// redirect the tail's data to the output
					final byte buff[] = new byte[
						pendingAugmentSize < LoadExecutor.BUFF_SIZE_HI ?
							(int) pendingAugmentSize : LoadExecutor.BUFF_SIZE_HI
					];
					final int
						countPages = (int) (pendingAugmentSize / buff.length),
						countTailBytes = (int) (pendingAugmentSize % buff.length);
					//
					for(int i = 0; i < countPages; i++) {
						if(read(buff) == buff.length) {
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
				}
				// clean up the appending on success
				pendingAugmentSize = 0;
				maskRangesHistory.or(maskRangesPending);
				maskRangesPending.clear();
			}
		}
	}
	//
	@Override
	public final InputStream getAugmentContent()
	throws IOException {
		InputStream augment;
		if(pendingAugmentSize > 0) {
			synchronized(this) {
				if(maskRangesHistory.get(getRangeCount(size) - 1)) { // write from next layer
					augment = new UniformData(
						offset + size, pendingAugmentSize, currLayerIndex.get() + 1, UniformDataSource.DEFAULT
					);
				} else { // write from current layer
					setRelativeOffset(size);
					// change the size
					size += pendingAugmentSize;
					augment = this;
				}
				// clean up the appending on success
				pendingAugmentSize = 0;
				maskRangesHistory.or(maskRangesPending);
				maskRangesPending.clear();
			}
		} else {
			throw new IllegalStateException("No append scheduled");
		}
		return augment;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
