package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.item.data.FileItem;
//
import java.util.concurrent.RunnableFuture;
/**
 Created by andrey on 22.11.15.
 */
public interface FileIoTask<T extends FileItem>
extends DataIOTask<T>, RunnableFuture<FileIoTask<T>> {
}
