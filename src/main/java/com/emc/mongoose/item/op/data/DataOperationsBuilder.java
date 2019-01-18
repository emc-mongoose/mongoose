package com.emc.mongoose.item.op.data;

import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.op.OperationsBuilder;
import com.github.akurilov.commons.collection.Range;
import java.util.List;

/** Created by kurila on 27.09.16. */
public interface DataOperationsBuilder<I extends DataItem, O extends DataOperation<I>>
    extends OperationsBuilder<I, O> {

  DataOperationsBuilder<I, O> fixedRanges(final List<Range> fixedRanges);

  DataOperationsBuilder<I, O> randomRangesCount(final int count);

  DataOperationsBuilder<I, O> sizeThreshold(final long sizeThreshold);

  DataOperationsBuilder<I, O> srcItemsCount(final int min, final int max);

  DataOperationsBuilder<I, O> srcItemsForConcat(final List<I> items);

  List<Range> fixedRanges();

  int randomRangesCount();

  long sizeThreshold();
}
