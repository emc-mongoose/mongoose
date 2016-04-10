package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedHttpDataLoadSvc<T extends HttpDataItem>
extends HttpDataLoadSvc<T>, MixedDataLoadSvc<T> {
}
