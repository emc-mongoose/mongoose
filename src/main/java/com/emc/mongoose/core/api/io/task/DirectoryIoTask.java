package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
//
import java.util.concurrent.RunnableFuture;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryIoTask<T extends FileItem, C extends Directory<T>>
extends ContainerIoTask<T, C>, RunnableFuture<DirectoryIoTask<T, C>> {
}
