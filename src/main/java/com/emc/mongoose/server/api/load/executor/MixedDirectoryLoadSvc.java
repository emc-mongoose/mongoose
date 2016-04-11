package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedDirectoryLoadSvc<F extends FileItem, D extends Directory<F>>
extends DirectoryLoadSvc<F, D>, MixedContainerLoadSvc<F, D> {
}
