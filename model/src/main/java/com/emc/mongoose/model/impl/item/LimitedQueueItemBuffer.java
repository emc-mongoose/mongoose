package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemBuffer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
/**
 The blocking queue wrapped in order to act as data item output from the tail and as data item input
 from the head.
 */
public class LimitedQueueItemBuffer<T extends Item>
implements ItemBuffer<T> {
	//
	private T lastItem = null;
	protected final BlockingQueue<T> queue;
	//
	public LimitedQueueItemBuffer(final BlockingQueue<T> queue) {
		this.queue = queue;
	}
	/**
	 Non-blocking put implementation
	 @param item the data item to put
	 @throws IOException if no free capacity in the buffer
	 */
	@Override
	public void put(final T item)
	throws IOException {
		if(!queue.offer(item)) {
			throw new IOException("Buffer has no free space to put an item");
		}
	}
	/**
	 Non-blocking bulk put implementation
	 @param buffer the buffer containing the data items to put
	 @return the count of the items been written, may return less count than specified if not enough
	 free capacity is in the queue
	 @throws IOException doesn't throw
	 */
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		int i = from;
		while(i < to && queue.offer(buffer.get(i))) {
			i ++;
		}
		return i - from;
	}
	//
	@Override
	public final int put(final List<T> items)
	throws IOException {
		return put(items, 0, items.size());
	}
	/**
	 @return self
	 @throws IOException doesn't throw
	 */
	@Override
	public
	LimitedQueueItemBuffer<T> getInput()
	throws IOException {
		return this;
	}
	/**
	 Non-blocking get implementation
	 @return the data item or null if the buffer is empty
	 @throws IOException doesn't throw
	 */
	@Override
	public T get()
	throws IOException {
		return queue.poll();
	}
	/**
	 Non-blocking bulk get implementation
	 @param maxCount the count limit
	 @param buffer buffer for the data items
	 @return the count of the items been get
	 @throws IOException if something goes wrong
	 */
	@Override
	public int get(final List<T> buffer, final int maxCount)
	throws IOException {
		try {
			return queue.drainTo(buffer, maxCount);
		} catch(final UnsupportedOperationException | IllegalArgumentException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public void skip(final long itemsCount)
	throws IOException {
		try {
			T item;
			for(int i = 0; i < itemsCount; i++) {
				item = queue.take();
				if(item.equals(lastItem)) {
					return;
				}
			}
		} catch (final InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}
	//
	@Override
	public final boolean isEmpty() {
		return queue.isEmpty();
	}
	//
	@Override
	public final int size() {
		return queue.size();
	}
	/**
	 Does nothing
	 @throws IOException doesn't throw
	 */
	@Override
	public void reset()
	throws IOException {
	}
	/**
	 Does nothing
	 @throws IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
	}
	//
	@Override
	public String toString() {
		return "itemsQueue#" + hashCode() + "";
	}
}
