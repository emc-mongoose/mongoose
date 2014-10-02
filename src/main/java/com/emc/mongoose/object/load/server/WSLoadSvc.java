package com.emc.mongoose.object.load.server;
//
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadSvc<T extends WSObject>
extends ObjectLoadExecutor<T> {
}
