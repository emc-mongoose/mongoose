package com.emc.mongoose.server.api.load.builder;
import com.emc.mongoose.core.api.v1.item.container.Directory;
import com.emc.mongoose.core.api.v1.item.data.FileItem;
import com.emc.mongoose.core.api.v1.load.builder.DirectoryLoadBuilder;
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadBuilderSvc<
	T extends FileItem, C extends Directory<T>, U extends DirectoryLoadSvc<T, C>
> extends DirectoryLoadBuilder<T, C, U>, ContainerLoadBuilderSvc<T, C, U> {
}
