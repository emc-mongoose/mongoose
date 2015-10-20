package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.AppendableDataItem;
import com.emc.mongoose.core.api.data.UpdatableDataItem;
/**
 Created by kurila on 29.09.14.
 Identifiable, appendable and updatable data item.
 Data item identifier is a 64-bit word.
 */
public interface MutableDataItem
extends AppendableDataItem, UpdatableDataItem {
}
