package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadType;

import java.util.List;
/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, O extends IoTask<I>> {

	IoTaskBuilder<I, O> setIoType(final LoadType ioType);

	IoTaskBuilder<I, O> setSrcPath(final String srcPath);

	O getInstance(final I item, final String dstPath);

	List<O> getInstances(final List<I> items, final int from, final int to);

	List<O> getInstances(final List<I> items, String dstPath, final int from, final int to);

	List<O> getInstances(
		final List<I> items, final List<String> dstPaths, final int from, final int to
	);
}
