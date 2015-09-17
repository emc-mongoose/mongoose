package com.emc.mongoose.core.impl.data;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataCorruptionException;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.AppendableDataItem;
import com.emc.mongoose.core.api.data.DataSizeException;
import com.emc.mongoose.core.api.data.UpdatableDataItem;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
//
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
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
		FMT_MSG_MERGE_MASKS = "{}: move pending ranges \"{}\" to history \"{}\"",
		STR_EMPTY_MASK = "0";
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final BitSet
		maskRangesRead = new BitSet(Long.SIZE),
		maskRangesWrite[] = new BitSet[] { new BitSet(Long.SIZE), new BitSet(Long.SIZE)};
	protected int currLayerIndex = 0;
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
	private final static ThreadLocal<StringBuilder> THR_LOCAL_STR_BUILDER = new ThreadLocal<>();
	//
	@Override
	public synchronized String toString() {
		StringBuilder strBuilder = THR_LOCAL_STR_BUILDER.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THR_LOCAL_STR_BUILDER.set(strBuilder);
		} else {
			strBuilder.setLength(0); // reset
		}
		return strBuilder
			.append(super.toString()).append(RunTimeConfig.LIST_SEP)
			.append(Integer.toHexString(currLayerIndex)).append('/')
			.append(
				maskRangesRead.isEmpty() ? STR_EMPTY_MASK :
				Hex.encodeHexString(maskRangesRead.toByteArray())
			).toString();
	}
	//
	@Override
	public synchronized void fromString(final String v)
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
				currLayerIndex = Integer.valueOf(rangesInfo.substring(0, sepPos), 0x10);
				setDataSource(UniformDataSource.DEFAULT, currLayerIndex);
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
				maskRangesRead.or(BitSet.valueOf(Hex.decodeHex(rangesMaskChars)));
			} catch(final DecoderException | NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_MASK, rangesInfo));
			}
		} else {
			throw new IllegalArgumentException(String.format(FMT_MSG_INVALID_RECORD, v));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof RangeLayerData) || !super.equals(o)) {
			return false;
		} else {
			final RangeLayerData other = RangeLayerData.class.cast(o);
			return maskRangesRead.equals(other.maskRangesRead)
				&& maskRangesWrite.equals(other.maskRangesWrite);
		}
	}
	//
	@Override
	public int hashCode() {
		return super.hashCode() ^ maskRangesRead.hashCode() ^ maskRangesWrite.hashCode();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(currLayerIndex);
		out.writeLong(maskRangesRead.isEmpty() ? 0 : maskRangesRead.toLongArray()[0]);
		out.writeLong(maskRangesWrite[0].isEmpty() ? 0 : maskRangesWrite[0].toLongArray()[0]);
		out.writeLong(maskRangesWrite[1].isEmpty() ? 0 : maskRangesWrite[1].toLongArray()[0]);
	}
	//
	@Override
	public synchronized void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		currLayerIndex = in.readInt();
		maskRangesRead.or(BitSet.valueOf(new long[]{in.readLong()}));
		maskRangesWrite[0].or(BitSet.valueOf(new long[] {in.readLong()}));
		maskRangesWrite[1].or(BitSet.valueOf(new long[] {in.readLong()}));
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
	public final int getCurrLayerIndex() {
		return currLayerIndex;
	}
	//
	@Override
	public final synchronized long readAndVerifyFully(final ReadableByteChannel chanSrc)
	throws DataSizeException, DataCorruptionException, IOException {
		// do not go over ranges if there's no updated ones
		if(maskRangesRead.isEmpty()) {
			if(currLayerIndex == 0) {
				return super.readAndVerifyFully(chanSrc);
			} else {
				return new UniformData(offset, size, currLayerIndex, UniformDataSource.DEFAULT)
					.readAndVerifyFully(chanSrc);
			}
		}
		//
		final int countRangesTotal = size > 0 ? getRangeCount(size) : Integer.MAX_VALUE;
		long rangeOffset, rangeSize, byteCount = 0;
		UniformData updatedRange;
		for(int i = 0; i < countRangesTotal; i ++) {
			rangeOffset = getRangeOffset(i);
			rangeSize = getRangeSize(i);
			try {
				if(maskRangesRead.get(i)) {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "{}: range #{} has been modified",
							Long.toHexString(offset), i
						);
					}
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex + 1,
						UniformDataSource.DEFAULT
					);
					byteCount += updatedRange.readAndVerifyRangeFully(chanSrc, 0, rangeSize);
				} else if(currLayerIndex > 0) {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "{}: range #{} contains previous layer of data",
							Long.toHexString(offset), i
						);
					}
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex,
						UniformDataSource.DEFAULT
					);
					byteCount += updatedRange.readAndVerifyRangeFully(chanSrc, 0, rangeSize);
				} else {
					byteCount += readAndVerifyRangeFully(chanSrc, rangeOffset, rangeSize);
				}
			} catch(final DataSizeException | DataCorruptionException e) {
				e.offset += getRangeOffset(i);
				throw e;
			}
		}
		return byteCount;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// UPDATE //////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean hasBeenUpdated() {
		return !maskRangesRead.isEmpty();
	}
	//
	@Override
	public final boolean hasScheduledUpdates() {
		return !maskRangesWrite[0].isEmpty() || !maskRangesWrite[1].isEmpty();
	}
	//
	@Override
	public final boolean isCurrLayerRangeUpdated(final int i) {
		return maskRangesRead.get(i);
	}
	//
	@Override
	public final boolean isCurrLayerRangeUpdating(final int i) {
		return maskRangesWrite[0].get(i);
	}
	//
	@Override
	public final boolean isNextLayerRangeUpdating(final int i) {
		return maskRangesWrite[1].get(i);
	}
	//
	@Override
	public final synchronized void scheduleRandomUpdate() {
		final int
			countRangesTotal = getRangeCount(size),
			startCellPos = ThreadLocalRandom.current().nextInt(countRangesTotal);
		int nextCellPos;
		if(countRangesTotal > maskRangesRead.cardinality() + maskRangesWrite[0].cardinality()) {
			// current layer has not updated yet ranges
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!maskRangesRead.get(nextCellPos)) {
					if(!maskRangesWrite[0].get(nextCellPos)) {
						maskRangesWrite[0].set(nextCellPos);
						break;
					}
				}
			}
		} else {
			// update the next layer ranges
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!maskRangesWrite[0].get(nextCellPos)) {
					if(!maskRangesWrite[1].get(nextCellPos)) {
						maskRangesWrite[1].set(nextCellPos);
						break;
					}
				}
			}
		}
	}
	//
	@Override
	public final void scheduleRandomUpdates(final int count)
	throws IllegalArgumentException {
		final int countRangesTotal = getRangeCount(size);
		if(count < 1 || count > countRangesTotal) {
			throw new IllegalArgumentException(
				"Range count should be more than 0 and less than max " + countRangesTotal +
				" for the item size"
			);
		}
		for(int i = 0; i < count; i++) {
			scheduleRandomUpdate();
		}
	}
	//
	@Override
	public final long getUpdatingRangesSize() {
		final long rangeCount = getRangeCount(size);
		long pendingSize = 0;
		for(int i = 0; i < rangeCount; i ++) {
			if(maskRangesWrite[0].get(i) || maskRangesWrite[1].get(i)) {
				pendingSize += getRangeSize(i);
			}
		}
		return pendingSize;
	}
	//
	@Override
	public final synchronized long writeUpdatedRangesFully(final WritableByteChannel chanOut)
	throws IOException {
		BitSet layerMask;
		long rangeOffset, rangeSize, byteCount = 0;
		DataItem nextRangeData;
		for(int i = 0; i < maskRangesWrite.length; i ++) {
			layerMask = maskRangesWrite[i];
			for(int j = layerMask.nextSetBit(0); j >= 0; j = layerMask.nextSetBit(j + 1)) {
				rangeOffset = getRangeOffset(j);
				rangeSize = getRangeSize(j);
				nextRangeData = new UniformData(
					offset + rangeOffset, rangeSize, currLayerIndex + i + 1,
					UniformDataSource.DEFAULT
				);
				byteCount += nextRangeData.writeFully(chanOut);
			}
		}
		//
		commitUpdatedRanges();
		return byteCount;
	}
	//
	@Override
	public final void commitUpdatedRanges() {
		// move pending updated ranges to history
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, FMT_MSG_MERGE_MASKS,
				Long.toHexString(offset),
				Hex.encodeHexString(maskRangesWrite[0].toByteArray()),
				Hex.encodeHexString(maskRangesRead.toByteArray())
			);
		}
		if(maskRangesWrite[1].isEmpty()) {
			maskRangesRead.or(maskRangesWrite[0]);
		} else {
			maskRangesRead.clear();
			maskRangesRead.or(maskRangesWrite[1]);
			maskRangesWrite[1].clear();
			currLayerIndex ++;
		}
		maskRangesWrite[0].clear();
	}
	//
	@Override
	public final void resetUpdates() {
		maskRangesRead.clear();
		maskRangesWrite[0].clear();
		maskRangesWrite[1].clear();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// APPEND //////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean isAppending() {
		return pendingAugmentSize > 0;
	}
	//
	@Override
	public void scheduleAppend(final long augmentSize)
	throws IllegalArgumentException {
		if(augmentSize > 0) {
			pendingAugmentSize = augmentSize;
			final int
				lastCellPos = size > 0 ? getRangeCount(size) - 1 : 0,
				nextCellPos = getRangeCount(size + augmentSize);
			if(lastCellPos < nextCellPos && maskRangesRead.get(lastCellPos)) {
				maskRangesRead.set(lastCellPos, nextCellPos);
			}
		} else {
			throw new IllegalArgumentException(
				"Append tail size should be more than 0, but got " + augmentSize
			);
		}
	}
	//
	@Override
	public final long getAppendSize() {
		return pendingAugmentSize;
	}
	//
	@Override
	public final void commitAppend() {
		size += pendingAugmentSize;
		pendingAugmentSize = 0;
	}
	//
	@Override @Deprecated
	public final synchronized long writeAugmentFully(final WritableByteChannel chanOut)
	throws IOException {
		long byteCount = 0;
		if(pendingAugmentSize > 0) {
			final int rangeIndex = size > 0 ? getRangeCount(size) - 1 : 0;
			final UniformData augmentData;
			if(maskRangesRead.get(rangeIndex)) { // write from the next layer
				augmentData = new UniformData(
					offset + size, pendingAugmentSize, currLayerIndex + 1,
					UniformDataSource.DEFAULT
				);
				byteCount += augmentData.writeFully(chanOut);
			} else if(currLayerIndex > 0) { // write from the current layer
				augmentData = new UniformData(
					offset + size, pendingAugmentSize, currLayerIndex,
					UniformDataSource.DEFAULT
				);
				byteCount += augmentData.writeFully(chanOut);
			} else { // write from the zero layer
				augmentData = this;
				byteCount += augmentData.writeRangeFully(chanOut, size, pendingAugmentSize);
			}
			// clean up the appending on success
			commitAppend();
		}
		return byteCount;
	}
}
