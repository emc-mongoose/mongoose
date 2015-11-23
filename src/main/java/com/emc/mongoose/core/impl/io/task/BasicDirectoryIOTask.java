package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIOTask;
/**
 Created by kurila on 23.11.15.
 */
public class BasicDirectoryIOTask<
	T extends FileItem, C extends Directory<T>, X extends IOConfig<T, C>
> extends BasicIOTask<C, C, X>
implements DirectoryIOTask<T, C> {
	//
	public BasicDirectoryIOTask(final C item, final String nodeAddr, final X ioConfig) {
		super(item, nodeAddr, ioConfig);
	}
}
