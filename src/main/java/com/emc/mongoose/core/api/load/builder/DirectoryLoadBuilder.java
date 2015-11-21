package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadBuilder<
	T extends FileItem, C extends Directory<T>, U extends DirectoryLoadExecutor<T, C>
> extends ContainerLoadBuilder<T, C, U> {
}
