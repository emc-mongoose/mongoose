package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.IoTask;
/**
 Created by andrey on 09.04.16.
 */
public interface DirectoryLoadExecutor<F extends FileItem, D extends Directory<F>, A extends IoTask<D>>
extends ContainerLoadExecutor<F, D, A> {
}
