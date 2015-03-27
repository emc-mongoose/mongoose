package com.emc.mongoose.client.api.persist;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.persist.DataItemBuffer;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.persist.DataItemBufferSvc;
//
import java.util.Map;
/**
 Created by kurila on 14.11.14.
 */
public interface DataItemBufferClient<T extends DataItem>
extends DataItemBuffer<T>, Map<String, DataItemBufferSvc<T>> {
}
