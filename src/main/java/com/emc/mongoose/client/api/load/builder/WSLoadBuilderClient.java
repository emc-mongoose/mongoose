package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.WSObject;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderClient<T extends WSObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilderClient<T, U> {
}
