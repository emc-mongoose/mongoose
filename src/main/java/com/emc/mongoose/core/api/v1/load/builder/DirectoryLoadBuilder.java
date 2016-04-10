package com.emc.mongoose.core.api.v1.load.builder;
//
import com.emc.mongoose.core.api.v1.item.container.Directory;
import com.emc.mongoose.core.api.v1.item.data.FileItem;
import com.emc.mongoose.core.api.v1.load.executor.DirectoryLoadExecutor;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadBuilder<
	T extends FileItem, C extends Directory<T>, U extends DirectoryLoadExecutor<T, C>
> extends ContainerLoadBuilder<T, C, U> {
}
