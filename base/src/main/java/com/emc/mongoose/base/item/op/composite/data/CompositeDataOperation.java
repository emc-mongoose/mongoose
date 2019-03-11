package com.emc.mongoose.base.item.op.composite.data;

import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.op.composite.CompositeOperation;
import com.emc.mongoose.base.item.op.partial.data.PartialDataOperation;
import java.util.List;

/** Created by andrey on 25.11.16. */
public interface CompositeDataOperation<I extends DataItem> extends CompositeOperation<I> {

	@Override
	List<? extends PartialDataOperation<I>> subOperations();
}
