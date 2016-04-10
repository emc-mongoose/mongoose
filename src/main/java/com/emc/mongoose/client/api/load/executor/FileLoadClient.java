package com.emc.mongoose.client.api.load.executor;
import com.emc.mongoose.core.api.v1.item.data.FileItem;
import com.emc.mongoose.core.api.v1.load.executor.FileLoadExecutor;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadClient<T extends FileItem, W extends FileLoadSvc<T>>
extends DataLoadClient<T, W>, FileLoadExecutor<T> {
}
