package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;

import java.rmi.RemoteException;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverSvc<I extends Item, O extends IoTask<I, R>, R extends IoResult<I>>
extends StorageDriver<I, O, R>, Service {
}
