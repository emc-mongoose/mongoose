package com.emc.mongoose.client.api.load.builder;
import com.emc.mongoose.client.api.load.executor.DirectoryLoadClient;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.builder.DirectoryLoadBuilder;
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadBuilderClient<
	T extends FileItem,
	C extends Directory<T>,
	W extends DirectoryLoadSvc<T, C>,
	U extends DirectoryLoadClient<T, C, W>
> extends ContainerLoadBuilderClient<T, C, W, U>, DirectoryLoadBuilder<T, C, U> {
}
