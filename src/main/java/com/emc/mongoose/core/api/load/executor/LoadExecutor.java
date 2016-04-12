package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.load.generator.LoadGenerator;
//
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 28.04.14.
 A mechanism of data items load execution.
 May be a consumer and producer both also.
 Supports method "join" for waiting the load execution to be done.
 */
public interface LoadExecutor<T extends Item, A extends IoTask<T>>
extends Closeable {
	//
	int DEFAULT_INTERNAL_BATCH_SIZE = 0x80;
	//
	int submit(
		final LoadGenerator<T, A> loadGenerator, final List<A> ioTasks, final int from, final int to
	) throws IOException;
}
