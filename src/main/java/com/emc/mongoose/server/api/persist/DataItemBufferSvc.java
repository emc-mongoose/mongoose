package com.emc.mongoose.server.api.persist;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.persist.DataItemBuffer;
import com.emc.mongoose.server.api.Service;
/**
 Created by kurila on 17.11.14.
 */
public interface DataItemBufferSvc<T extends DataItem>
extends DataItemBuffer<T>, Service {
}
