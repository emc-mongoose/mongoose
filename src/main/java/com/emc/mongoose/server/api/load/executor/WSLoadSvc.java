package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSDataLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadSvc<T extends WSObject>
extends WSDataLoadExecutor<T>, LoadSvc<T> {
}
