package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadSvc<T extends DataObject>
extends ObjectLoadExecutor<T>, LoadSvc<T> {
}
