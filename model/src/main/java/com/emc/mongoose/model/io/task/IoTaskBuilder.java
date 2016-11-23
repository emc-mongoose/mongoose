package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.util.List;
/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, R extends IoResult, O extends IoTask<I, R>> {

	IoType getIoType();

	IoTaskBuilder<I, R, O> setIoType(final IoType ioType);

	String getSrcPath();

	IoTaskBuilder<I, R, O> setSrcPath(final String srcPath);

	O getInstance(final I item, final String dstPath);

	List<O> getInstances(final List<I> items, final int from, final int to);

	List<O> getInstances(final List<I> items, String dstPath, final int from, final int to);

	List<O> getInstances(
		final List<I> items, final List<String> dstPaths, final int from, final int to
	);
}
