package com.emc.mongoose.core.api.load.executor;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadExecutor<T extends FileItem, C extends Directory<T>>
extends ContainerLoadExecutor<T, C> {
}
