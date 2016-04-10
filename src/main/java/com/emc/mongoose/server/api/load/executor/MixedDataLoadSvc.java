package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.v1.item.data.DataItem;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedDataLoadSvc<T extends DataItem>
extends DataLoadSvc<T>, MixedLoadSvc<T> {
}
