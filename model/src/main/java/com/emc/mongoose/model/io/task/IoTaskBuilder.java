package com.emc.mongoose.model.io.task;

import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, O extends IoTask<I, R>, R extends IoResult> {
	
	int getOriginCode();
	
	IoType getIoType();

	IoTaskBuilder<I, O, R> setIoType(final IoType ioType);

	String getSrcPath();

	IoTaskBuilder<I, O, R> setSrcPath(final String srcPath);

	O getInstance(final I item, final String dstPath)
	throws IOException;

	List<O> getInstances(final List<I> items)
	throws IOException;

	List<O> getInstances(final List<I> items, String dstPath)
	throws IOException;

	List<O> getInstances(final List<I> items, final List<String> dstPaths)
	throws IOException;
}
