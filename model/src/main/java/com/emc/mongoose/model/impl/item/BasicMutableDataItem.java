package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.MutableDataItem;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 Created on 19.07.16.
 */
public class BasicMutableDataItem
extends BasicDataItem
implements MutableDataItem {

	private static final Logger LOG = LogManager.getLogger();
	private static final char LAYER_MASK_SEP = '/';

	private static final ThreadLocal<StringBuilder> THREAD_LOCAL_CONTAINER = new ThreadLocal<>();
	private static final double LOG2 = Math.log(2);

	private static final String
		FMT_MSG_MASK = "Ranges mask is not correct hexadecimal value: %s",
		FMT_MSG_MERGE_MASKS = "{}: move pending ranges \"{}\" to history \"{}\"",
		STR_EMPTY_MASK = "0";

	protected final BitSet maskRangesRead = new BitSet(Long.SIZE);
	private final BitSet[] maskRangesWrite = new BitSet[] { new BitSet(Long.SIZE), new BitSet(Long.SIZE)};
	protected int currLayerIndex = 0;
	private long pendingAugmentSize = 0;

	public BasicMutableDataItem(final ContentSource contentSrc) {
		super(contentSrc); // ranges remain uninitialized
	}

	public BasicMutableDataItem(final String metaInfo, final ContentSource contentSrc) {
		super(
			metaInfo.substring(0, metaInfo.lastIndexOf(",")),
			contentSrc
		);
		//
		final String rangesInfo = metaInfo.substring(
			metaInfo.lastIndexOf(",") + 1, metaInfo.length()
		);
		final int sepPos = rangesInfo.indexOf(LAYER_MASK_SEP);
		try {
			// extract hexadecimal layer number
			currLayerIndex = Integer.valueOf(rangesInfo.substring(0, sepPos), 0x10);
			setContentSource(contentSrc, currLayerIndex);
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

	public BasicMutableDataItem(
		final Long offset, final Long size, final ContentSource contentSrc
	) {
		super(offset, size, contentSrc);
	}

	public BasicMutableDataItem(
		final String name, final Long offset, final Long size, Integer layerNum,
		final ContentSource contentSrc
	) {
		super(name, offset, size, layerNum, contentSrc);
	}

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

	public static int getRangeCount(final long size) {
		return (int) Math.ceil(Math.log(size + 1) / LOG2);
	}
	//
	public static long getRangeOffset(final int i) {
		return (1 << i) - 1;
	}

	@Override
	public long getRangeSize(final int i) {
		return Math.min(getRangeOffset(i + 1), size) - getRangeOffset(i);
	}

	@Override
	public int getCountRangesTotal() {
		return getRangeCount(size);
	}

	@Override
	public int getCurrLayerIndex() {
		return currLayerIndex;
	}

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

	@Override
	public boolean isAppending() {
		return false;
	}

	@Override
	public long getAppendSize() {
		return pendingAugmentSize;
	}

	@Override
	public void commitAppend() {
		size += pendingAugmentSize;
		pendingAugmentSize = 0;
	}

	@Override
	public boolean hasBeenUpdated() {
		return !maskRangesRead.isEmpty();
	}

	@Override
	public boolean hasScheduledUpdates() {
		return !maskRangesWrite[0].isEmpty() || !maskRangesWrite[1].isEmpty();
	}

	@Override
	public boolean isCurrLayerRangeUpdated(final int layerNum) {
		return maskRangesRead.get(layerNum);
	}

	@Override
	public boolean isCurrLayerRangeUpdating(final int layerNum) {
		return maskRangesWrite[0].get(layerNum);
	}

	@Override
	public boolean isNextLayerRangeUpdating(final int layerNum) {
		return maskRangesWrite[1].get(layerNum);
	}

	private synchronized void scheduleRandomUpdate(final int countRangesTotal)
	throws IllegalArgumentException, IllegalStateException {
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

	@Override
	public void scheduleRandomUpdates(final int count)
	throws IllegalArgumentException, IllegalStateException {
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

	@Override
	public long getUpdatingRangesSize() {
		final long rangeCount = getRangeCount(size);
		long pendingSize = 0;
		for(int i = 0; i < rangeCount; i ++) {
			if(maskRangesWrite[0].get(i) || maskRangesWrite[1].get(i)) {
				pendingSize += getRangeSize(i);
			}
		}
		return pendingSize;
	}

	@Override
	public void commitUpdatedRanges() {
		// move pending updated ranges to history TODO move logging
//		if(LOG.isTraceEnabled(Markers.MSG)) {
//			LOG.trace(
//				Markers.MSG, FMT_MSG_MERGE_MASKS,
//				Long.toHexString(offset),
//				Hex.encodeHexString(maskRangesWrite[0].toByteArray()),
//				Hex.encodeHexString(maskRangesRead.toByteArray())
//			);
//		}
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

	@Override
	public void resetUpdates() {
		maskRangesRead.clear();
		maskRangesWrite[0].clear();
		maskRangesWrite[1].clear();
	}

	@Override
	public String toString() {
		StringBuilder strBuilder = THREAD_LOCAL_CONTAINER.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THREAD_LOCAL_CONTAINER.set(strBuilder);
		} else {
			strBuilder.setLength(0); // reset
		}
		return strBuilder
			.append(super.toString()).append(',')
			.append(Integer.toHexString(currLayerIndex)).append('/')
			.append(
				maskRangesRead.isEmpty() ? STR_EMPTY_MASK :
					Hex.encodeHexString(maskRangesRead.toByteArray())
			).toString();
	}
}
