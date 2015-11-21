package com.emc.mongoose.client.api.load.executor;
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface DirectoryLoadClient<
	T extends FileItem, C extends Directory<T>, W extends DirectoryLoadSvc<T, C>
> extends ContainerLoadClient<T, C, W> {
}
