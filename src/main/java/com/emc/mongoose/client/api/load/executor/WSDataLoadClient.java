package com.emc.mongoose.client.api.load.executor;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSDataLoadExecutor;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadClient<T extends WSObject, W extends WSDataLoadSvc<T>>
extends DataLoadClient<T, W>, WSDataLoadExecutor<T> {
}
