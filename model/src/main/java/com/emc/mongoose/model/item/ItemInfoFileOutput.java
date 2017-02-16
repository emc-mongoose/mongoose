package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.TextFileOutput;
import com.emc.mongoose.model.io.task.IoTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 09.01.17.
 */
public final class ItemInfoFileOutput<I extends Item, O extends IoTask>
implements Output<O> {
	
	private final Output<String> itemInfoOutput;
	
	public ItemInfoFileOutput(final Path filePath)
	throws IOException {
		itemInfoOutput = new TextFileOutput(filePath);
	}
	
	@Override
	public final boolean put(final O ioResult)
	throws IOException {
		return itemInfoOutput.put(ioResult.getItem().toString());
	}
	
	@Override
	public final int put(final List<O> ioResults, final int from, final int to)
	throws IOException {
		final int n = to - from;
		final List<String> itemsInfo = new ArrayList<>(n);
		for(int i = from; i < to; i ++) {
			itemsInfo.add(ioResults.get(i).getItem().toString());
		}
		return itemInfoOutput.put(itemsInfo, 0, n);
	}
	
	@Override
	public final int put(final List<O> ioResults)
	throws IOException {
		final List<String> itemsInfo = new ArrayList<>(ioResults.size());
		for(final O nextIoResult : ioResults) {
			itemsInfo.add(nextIoResult.getItem().toString());
		}
		return itemInfoOutput.put(itemsInfo);
	}
	
	@Override
	public final Input<O> getInput()
	throws IOException {
		throw new AssertionError();
	}
	
	@Override
	public final void close()
	throws IOException {
		itemInfoOutput.close();
	}
}
