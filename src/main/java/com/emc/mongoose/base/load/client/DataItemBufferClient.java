package com.emc.mongoose.base.load.client;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.DataItemBuffer;
import com.emc.mongoose.base.load.server.DataItemBufferSvc;
//
import java.util.Map;
/**
 Created by kurila on 14.11.14.
 */
public interface DataItemBufferClient<T extends DataItem>
extends DataItemBuffer<T>, Map<String, DataItemBufferSvc<T>> {
}
