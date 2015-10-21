package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSDataLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderSvc<T extends WSObject, U extends WSDataLoadExecutor<T>>
extends DataLoadBuilderSvc<T, U> {
}
