package com.emc.mongoose.client.api.load.builder;
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.load.builder.FileLoadBuilder;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
/**
 Created by andrey on 22.11.15.
 */
public interface FileLoadBuilderClient<
	T extends FileItem, W extends FileLoadSvc<T>, U extends FileLoadClient<T, W>
> extends DataLoadBuilderClient<T, W, U>, FileLoadBuilder<T, U> {
}
