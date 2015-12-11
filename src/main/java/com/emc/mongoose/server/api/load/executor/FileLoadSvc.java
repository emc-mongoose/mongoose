package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadSvc<T extends FileItem>
extends DataLoadSvc<T>, FileLoadExecutor<T> {
}
