package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.load.builder.ObjectLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilderSvc<T extends DataObject, U extends LoadExecutor<T>>
extends LoadBuilderSvc<T, U>, ObjectLoadBuilder<T, U> {
}
