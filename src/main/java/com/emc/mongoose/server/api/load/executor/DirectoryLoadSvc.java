package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadSvc<T extends FileItem, C extends Directory<T>>
extends ContainerLoadSvc<T, C>, DirectoryLoadExecutor<T, C> {
}
