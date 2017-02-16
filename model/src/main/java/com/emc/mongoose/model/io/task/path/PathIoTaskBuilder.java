package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.PathItem;

import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 31.01.17.
 */
public interface PathIoTaskBuilder<I extends PathItem, O extends PathIoTask<I>>
extends IoTaskBuilder<I, O> {

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
