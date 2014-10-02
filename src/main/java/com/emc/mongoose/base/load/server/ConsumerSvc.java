package com.emc.mongoose.base.load.server;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.remote.Service;
/**
 Created by kurila on 30.05.14.
 A remote/server-side data items consumer.
 */
public interface ConsumerSvc<T extends DataItem>
extends Consumer<T>, Service {
}
