package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.MutableDataItem;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

public class BasicMutableDataItem
extends BasicDataItem
implements MutableDataItem {
	//
	private final static char LAYER_MASK_SEP = '/';
	//
	protected final static String
		FMT_MSG_MASK = "Ranges mask is not correct hexadecimal value: %s",
		STR_EMPTY_MASK = "0";
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final BitSet maskRangesRead = new BitSet(Long.SIZE);
	protected transient final BitSet maskRangesWrite[] = new BitSet[] {
		new BitSet(Long.SIZE), new BitSet(Long.SIZE)
	};
	protected transient long pendingAugmentSize = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public BasicMutableDataItem() {
		super();
	}
	//
	public BasicMutableDataItem(final ContentSource contentSrc) {
		super(contentSrc); // ranges remain uninitialized
	}
	//
	public BasicMutableDataItem(final String value, final ContentSource contentSrc) {
		super(value.substring(0, value.lastIndexOf(",")), contentSrc);
		//
		final String rangesInfo = value.substring(value.lastIndexOf(",") + 1, value.length());
		final int sepPos = rangesInfo.indexOf(LAYER_MASK_SEP);
		try {
			// extract hexadecimal layer number
			layerNum = Integer.valueOf(rangesInfo.substring(0, sepPos), 0x10);
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
	}
	//
	public BasicMutableDataItem(
		final long offset, final long size, final ContentSource contentSrc
	) {
		super(offset, size, contentSrc);
	}
	//
	public BasicMutableDataItem(
		final String name, final long offset, final long size, final ContentSource contentSrc
	) {
		super(name, offset, size, 0, contentSrc);
	}
	//
	public BasicMutableDataItem(
		final String path, final String name, final long offset, final long size,
		final ContentSource contentSrc
	) {
		super(path, name, offset, size, 0, contentSrc);
	}
	//
	public BasicMutableDataItem(
		final String path, final String name, final long offset, final long size,
		final int layerNum, final ContentSource contentSrc
	) {
		super(path, name, offset, size, layerNum, contentSrc);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static ThreadLocal<StringBuilder> THR_LOCAL_STR_BUILDER = new ThreadLocal<>();
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
			.append(super.toString()).append(',')
			.append(Integer.toHexString(layerNum)).append('/')
			.append(
				maskRangesRead.isEmpty() ? STR_EMPTY_MASK :
					Hex.encodeHexString(maskRangesRead.toByteArray())
			).toString();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof BasicMutableDataItem) || !super.equals(o)) {
			return false;
		} else {
			final BasicMutableDataItem other = BasicMutableDataItem.class.cast(o);
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
		return Math.min(getRangeOffset(i + 1), size) - getRangeOffset(i);
	}
	//
	@Override
	public final int getCountRangesTotal() {
		return getRangeCount(size);
	}
	//
	@Override
	public final int getCurrLayerIndex() {
		return layerNum;
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
	private synchronized void scheduleRandomUpdate(final int countRangesTotal) {
		final int startCellPos = ThreadLocalRandom.current().nextInt(countRangesTotal);
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
			scheduleRandomUpdate(countRangesTotal);
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
	public final void commitUpdatedRanges() {
		// move pending updated ranges to history
		if(maskRangesWrite[1].isEmpty()) {
			maskRangesRead.or(maskRangesWrite[0]);
		} else {
			maskRangesRead.clear();
			maskRangesRead.or(maskRangesWrite[1]);
			maskRangesWrite[1].clear();
			layerNum ++;
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
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		final byte buff[] = maskRangesRead.toByteArray();
		out.writeInt(buff.length);
		out.write(buff);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		final int len = in.readInt();
		final byte buff[] = new byte[len];
		in.readFully(buff);
		maskRangesRead.clear();
		maskRangesRead.or(BitSet.valueOf(buff));
	}
}
