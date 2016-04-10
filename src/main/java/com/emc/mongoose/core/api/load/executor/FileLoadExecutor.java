package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.IoTask;
/**
 Created by andrey on 09.04.16.
 */
public interface FileLoadExecutor<F extends FileItem, A extends IoTask<F>>
extends MutableDataLoadExecutor<F, A> {
}
