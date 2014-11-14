package com.emc.mongoose.base.load.client;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.persist.DataItemBuffer;
//
import java.util.Map;
/**
 Created by kurila on 14.11.14.
 */
public interface DataItemBufferClient<T extends DataItem>
extends DataItemBuffer<T>, Map<String, DataItemBuffer<T>> {
}
