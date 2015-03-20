package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.load.builder.ObjectLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.WSObject;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilder<T extends WSObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilder<T, U> {
}
