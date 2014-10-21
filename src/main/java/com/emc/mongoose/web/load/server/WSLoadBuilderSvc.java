package com.emc.mongoose.web.load.server;
//
import com.emc.mongoose.object.load.server.ObjectLoadBuilderSvc;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderSvc<T extends WSObject, U extends WSLoadExecutor<T>>
extends ObjectLoadBuilderSvc<T, U> {
}
