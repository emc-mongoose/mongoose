package com.emc.mongoose.model.io.task.token;

import static com.emc.mongoose.model.io.task.token.TokenIoTask.TokenIoResult;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.TokenItem;

import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public interface TokenIoTaskBuilder<
	I extends TokenItem, O extends TokenIoTask<I, R>, R extends TokenIoResult<I>
>
extends IoTaskBuilder<I, O, R> {

	@Override
	O getInstance(final I item, final String dstPath)
	throws IOException;

	@Override
	List<O> getInstances(final List<I> items)
	throws IOException;

	@Override
	List<O> getInstances(final List<I> items, String dstPath)
	throws IOException;

	@Override
	List<O> getInstances(final List<I> items, final List<String> dstPaths)
	throws IOException;
}
