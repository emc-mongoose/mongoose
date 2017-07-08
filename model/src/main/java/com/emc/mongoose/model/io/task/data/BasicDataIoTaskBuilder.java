package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.composite.data.BasicCompositeDataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.storage.Credential;

import java.io.IOException;
import java.util.List;

import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;
import static com.emc.mongoose.model.item.DataItem.getRangeCount;

/**
 Created by kurila on 14.07.16.
 */
public class BasicDataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends BasicIoTaskBuilder<I, O>
implements DataIoTaskBuilder<I, O> {

	protected volatile List<ByteRange> fixedRanges = null;
	protected volatile int randomRangesCount = 0;
	protected volatile long sizeThreshold = 0;
	
	@Override
	public BasicDataIoTaskBuilder<I, O> setFixedRanges(final List<ByteRange> fixedRanges) {
		this.fixedRanges = fixedRanges;
		return this;
	}
	
	@Override
	public BasicDataIoTaskBuilder<I, O> setRandomRangesCount(final int count) {
		this.randomRangesCount = count;
		return this;
	}
	
	@Override
	public BasicDataIoTaskBuilder<I, O> setSizeThreshold(final long sizeThreshold) {
		this.sizeThreshold = sizeThreshold > 0 ? sizeThreshold : Long.MAX_VALUE;
		return this;
	}
	
	@Override
	public List<ByteRange> getFixedRanges() {
		return fixedRanges;
	}
	
	@Override
	public int getRandomRangesCount() {
		return randomRangesCount;
	}
	
	@Override
	public long getSizeThreshold() {
		return sizeThreshold;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem)
	throws IOException, IllegalArgumentException {
		final String uid;
		if(dataItem.size() > sizeThreshold) {
			if(randomRangesCount > 0 || (fixedRanges != null && fixedRanges.size() > 0)) {
				throw new IllegalArgumentException(
					"Not supported - both byte ranges configured and size threshold"
				);
			}
			return (O) new BasicCompositeDataIoTask<>(
				originCode, ioType, dataItem, inputPath, getNextOutputPath(),
				Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
				fixedRanges, randomRangesCount, sizeThreshold
			);
		} else {
			if(randomRangesCount > getRangeCount(dataItem.size())) {
				throw new IllegalArgumentException(
					"Configured random ranges count (" + randomRangesCount + ") is more than " +
						"allowed for the data item w/ size " + formatFixedSize(dataItem.size())
				);
			}
			return (O) new BasicDataIoTask<>(
				originCode, ioType, dataItem, inputPath, getNextOutputPath(),
				Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
				fixedRanges, randomRangesCount
			);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public void getInstances(final List<I> items, final List<O> buff)
	throws IOException, IllegalArgumentException {
		String uid;
		for(final I nextItem : items) {
			if(nextItem.size() > sizeThreshold) {
				if(randomRangesCount > 0 || (fixedRanges != null && fixedRanges.size() > 0)) {
					throw new IllegalArgumentException(
						"Not supported - both byte ranges configured and size threshold"
					);
				}
				buff.add(
					(O) new BasicCompositeDataIoTask<>(
						originCode, ioType, nextItem, inputPath, getNextOutputPath(),
						Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
						fixedRanges, randomRangesCount, sizeThreshold
					)
				);
			} else {
				if(randomRangesCount > getRangeCount(nextItem.size())) {
					throw new IllegalArgumentException(
						"Configured random ranges count (" + randomRangesCount + ") is more than " +
							"allowed for the data item w/ size " + formatFixedSize(nextItem.size())
					);
				}
				buff.add(
					(O) new BasicDataIoTask<>(
						originCode, ioType, nextItem, inputPath, getNextOutputPath(),
						Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
						fixedRanges, randomRangesCount
					)
				);
			}
		}
	}
}
