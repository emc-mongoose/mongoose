package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadBuilderSvc<T extends WSObject, U extends WSDataLoadSvc<T>>
extends DataLoadBuilderSvc<T, U> {
}
