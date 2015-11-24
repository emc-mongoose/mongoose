package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.FileIOTask;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIOTask<T extends FileItem, C extends Directory<T>, X extends IOConfig<T, C>>
extends BasicDataIOTask<T, C, X>
implements FileIOTask<T> {
	//
	public BasicFileIOTask(final T item, final X ioConfig) {
		super(item, null, ioConfig);
	}
	//
	@Override
	public void run()
	throws Exception {
		System.out.println(ioType + " \"" + item.getName() + "\"");
	}
	//
}
