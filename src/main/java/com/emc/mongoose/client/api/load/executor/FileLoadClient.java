package com.emc.mongoose.client.api.load.executor;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadClient<T extends FileItem, W extends FileLoadSvc<T>>
extends DataLoadClient<T, W>, FileLoadExecutor<T> {
}
