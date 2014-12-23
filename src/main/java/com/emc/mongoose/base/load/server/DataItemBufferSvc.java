package com.emc.mongoose.base.load.server;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.DataItemBuffer;
import com.emc.mongoose.util.remote.Service;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 17.11.14.
 */
public interface DataItemBufferSvc<T extends DataItem>
extends DataItemBuffer<T>, Service {
}
