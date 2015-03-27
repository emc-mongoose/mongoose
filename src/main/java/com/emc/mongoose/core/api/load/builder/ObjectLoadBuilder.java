package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilder<T extends DataObject, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
}
