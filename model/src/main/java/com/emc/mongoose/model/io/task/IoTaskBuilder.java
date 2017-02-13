package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, O extends IoTask<I>> {
	
	int getOriginCode();
	
	IoType getIoType();

	IoTaskBuilder<I, O> setIoType(final IoType ioType);

	String getSrcPath();

	IoTaskBuilder<I, O> setSrcPath(final String srcPath);

	O getInstance(final I item, final String dstPath)
	throws IOException;

	List<O> getInstances(final List<I> items)
	throws IOException;

	List<O> getInstances(final List<I> items, String dstPath)
	throws IOException;

	List<O> getInstances(final List<I> items, final List<String> dstPaths)
	throws IOException;
}
