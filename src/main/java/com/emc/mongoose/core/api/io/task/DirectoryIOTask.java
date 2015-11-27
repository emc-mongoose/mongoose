package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
//
import java.util.concurrent.RunnableFuture;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryIOTask<T extends FileItem, C extends Directory<T>>
extends ContainerIOTask<T, C>, RunnableFuture<DirectoryIOTask<T, C>> {
}
