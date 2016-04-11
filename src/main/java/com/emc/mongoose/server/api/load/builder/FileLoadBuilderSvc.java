package com.emc.mongoose.server.api.load.builder;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadBuilderSvc<T extends FileItem, U extends FileLoadSvc<T>>
extends DataLoadBuilderSvc<T, U> {
}
