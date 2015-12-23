package com.emc.mongoose.core.api.load.builder;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadBuilder<T extends FileItem, U extends FileLoadExecutor<T>>
extends DataLoadBuilder<T, U> {
}
