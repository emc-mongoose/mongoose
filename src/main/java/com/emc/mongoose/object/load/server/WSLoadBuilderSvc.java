package com.emc.mongoose.object.load.server;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.data.WSDataObject;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderSvc<T extends WSDataObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilderSvc<T, U> {
}
