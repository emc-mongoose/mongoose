package com.emc.mongoose.web.load.client;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.load.client.ObjectLoadBuilderClient;
import com.emc.mongoose.web.data.WSObject;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderClient<T extends WSObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilderClient<T, U> {
}
