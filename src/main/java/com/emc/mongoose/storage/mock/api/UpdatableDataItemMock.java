package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.UpdatableDataItem;
//
import java.util.List;
/**
 Created by kurila on 03.07.15.
 */
public interface UpdatableDataItemMock
extends UpdatableDataItem {
	public void updateRanges(final List<Long> ranges);
}
