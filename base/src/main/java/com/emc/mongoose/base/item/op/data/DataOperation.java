package com.emc.mongoose.base.item.op.data;

import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.op.Operation;
import com.github.akurilov.commons.collection.Range;
import java.util.BitSet;
import java.util.List;

/** Created by kurila on 11.07.16. */
public interface DataOperation<I extends DataItem> extends Operation<I> {

	@Override
	I item();

	void markRandomRanges(final int count);

	boolean hasMarkedRanges();

	long markedRangesSize();

	BitSet[] markedRangesMaskPair();

	List<Range> fixedRanges();

	int randomRangesCount();

	List<I> srcItemsToConcat();

	int currRangeIdx();

	void currRangeIdx(final int i);

	DataItem currRange();

	DataItem currRangeUpdate();

	long countBytesDone();

	void countBytesDone(long n);

	long respDataTimeStart();

	void startDataResponse() throws IllegalStateException;

	long dataLatency();
}
