package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.api.load.builder.WSDataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadBuilderSvc<T extends WSObject, U extends WSDataLoadSvc<T>>
extends WSDataLoadBuilder<T, U>, DataLoadBuilderSvc<T, U> {
}
