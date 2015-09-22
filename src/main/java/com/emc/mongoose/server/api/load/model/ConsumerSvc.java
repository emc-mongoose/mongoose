package com.emc.mongoose.server.api.load.model;
//
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.common.net.Service;

import java.rmi.RemoteException;
/**
 Created by kurila on 30.05.14.
 A remote/server-side data items consumer.
 */
public interface ConsumerSvc<T extends DataItem>
extends Consumer<T>, Service {
}
