package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.composite.data.BasicCompositeDataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.storage.Credential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem)
	throws IOException {
		final String uid;
		if(dataItem.size() > sizeThreshold) {
			return (O) new BasicCompositeDataIoTask<>(
				originCode, ioType, dataItem, inputPath, getNextOutputPath(),
				Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
				fixedRanges, randomRangesCount, sizeThreshold
			);
		} else {
			return (O) new BasicDataIoTask<>(
				originCode, ioType, dataItem, inputPath, getNextOutputPath(),
				Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
				fixedRanges, randomRangesCount
			);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		String uid;
		for(final I nextItem : items) {
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						originCode, ioType, nextItem, inputPath, getNextOutputPath(),
						Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
						fixedRanges, randomRangesCount, sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						originCode, ioType, nextItem, inputPath, getNextOutputPath(),
						Credential.getInstance(uid = getNextUid(), getNextSecret(uid)),
						fixedRanges, randomRangesCount
					)
				);
			}
		}
		return tasks;
	}
}
