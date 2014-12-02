package com.emc.mongoose.web.api;
//
import com.emc.mongoose.object.api.ObjectStorageClient;
import com.emc.mongoose.web.data.WSObject;
/**
 Created by kurila on 02.12.14.
 */
public interface WSClient<T extends WSObject>
extends ObjectStorageClient<T> {
}
