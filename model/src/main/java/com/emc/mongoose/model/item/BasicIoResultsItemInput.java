package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.IoBuffer;
import com.emc.mongoose.common.io.collection.LimitedQueueBuffer;
import com.emc.mongoose.model.io.task.IoTask.IoResult;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 Created by kurila on 16.01.17.
 */
public final class IoResultsItemInput<I extends Item, R extends IoResult<I>>
implements Input<I>, Output<R> {
	
	private final IoBuffer<I> itemBuff;
	
	public IoResultsItemInput(final BlockingQueue<I> itemQueue) {
		itemBuff = new LimitedQueueBuffer<>(itemQueue);
	}
	
	@Override
	public final boolean put(final R ioResult)
	throws IOException {
		return itemBuff.put(ioResult.getItem());
	}
	
	@Override
	public final int put(final List<R> buffer, final int from, final int to)
	throws IOException {
		final List<I> items = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			items.add(buffer.get(i).getItem());
		}
		return itemBuff.put(items);
	}
	
	@Override
	public final int put(final List<R> buffer)
	throws IOException {
		final List<I> items = new ArrayList<>(buffer.size());
		for(final R nextIoResult : buffer) {
			items.add(nextIoResult.getItem());
		}
		return itemBuff.put(items);
	}
	
	@Override
	public final Input<R> getInput()
	throws IOException {
		return null;
	}
	
	@Override
	public final I get()
	throws EOFException, IOException {
		return itemBuff.get();
	}
	
	@Override
	public final int get(final List<I> buffer, final int limit)
	throws IOException {
		return itemBuff.get(buffer, limit);
	}
	
	@Override
	public final long skip(final long count)
	throws IOException {
		return itemBuff.skip(count);
	}
	
	@Override
	public final void reset()
	throws IOException {
		itemBuff.reset();
	}
	
	@Override
	public final void close()
	throws IOException {
		itemBuff.close();
	}
}
