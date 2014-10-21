package com.emc.mongoose.web.load.client;
//
import com.emc.mongoose.object.load.client.ObjectLoadClient;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadClient<T extends WSObject>
extends ObjectLoadClient<T>, WSLoadExecutor<T> {
}
