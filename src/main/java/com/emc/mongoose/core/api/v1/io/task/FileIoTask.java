package com.emc.mongoose.core.api.v1.io.task;
//
import com.emc.mongoose.core.api.v1.item.data.FileItem;
//
import java.util.concurrent.RunnableFuture;
/**
 Created by andrey on 22.11.15.
 */
public interface FileIoTask<T extends FileItem>
extends DataIoTask<T>, RunnableFuture<FileIoTask<T>> {
}
