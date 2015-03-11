package com.emc.mongoose.client.api.load.executor;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadClient<T extends WSObject>
extends ObjectLoadClient<T>, WSLoadExecutor<T> {
}
