package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.concurrent.LifeCycle;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
/**
 Created by kurila on 24.09.15.
 */
public interface Consumer<T extends DataItem>
extends DataItemDst<T>, LifeCycle {
}
