package com.emc.mongoose.model.io.task;

import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.util.List;
/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, O extends IoTask<I, R>, R extends IoResult> {

	IoType getIoType();

	IoTaskBuilder<I, O, R> setIoType(final IoType ioType);

	String getSrcPath();

	IoTaskBuilder<I, O, R> setSrcPath(final String srcPath);

	O getInstance(final I item, final String dstPath);

	List<O> getInstances(final List<I> items, final int from, final int to);

	List<O> getInstances(final List<I> items, String dstPath, final int from, final int to);

	List<O> getInstances(
		final List<I> items, final List<String> dstPaths, final int from, final int to
	);
}
