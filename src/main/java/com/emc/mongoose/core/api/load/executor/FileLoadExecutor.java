package com.emc.mongoose.core.api.load.executor;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.load.executor.DataLoadExecutor;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadExecutor<T extends FileItem>
extends DataLoadExecutor<T> {
}
