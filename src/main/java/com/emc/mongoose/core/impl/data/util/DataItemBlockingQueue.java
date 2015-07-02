package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.util.DataItemInput;
import com.emc.mongoose.core.api.data.util.DataItemOutput;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
/**
 The blocking queue wrapped in order to act as data item output from the tail and as data item input
 from the head.
 */
public class DataItemBlockingQueue<T extends DataItem>
implements DataItemOutput<T>, DataItemInput<T> {
	//
	protected final BlockingQueue<T> queue;
	//
	public DataItemBlockingQueue(final BlockingQueue<T> queue) {
		this.queue = queue;
	}
	//
	@Override
	public void write(final T dataItem)
	throws IOException {
		try {
			queue.add(dataItem);
		} catch(final IllegalStateException e) {
			throw new IOException("Queue has no free capacity");
		}
	}
	//
	@Override
	public int write(final List<T> buffer)
	throws IOException {
		try {
			queue.addAll(buffer);
		} catch(final IllegalStateException e) {
			throw new IOException("Queue has no enough free capacity for bulk insertion");
		}
		return buffer.size();
	}
	//
	@Override
	public DataItemBlockingQueue<T> getInput()
	throws IOException {
		return this;
	}
	//
	@Override
	public T read()
	throws EOFException, IOException {
		try {
			return queue.remove();
		} catch(final NoSuchElementException e) {
			throw new IOException("Queue is empty");
		}
	}
	//
	@Override
	public int read(final List<T> buffer)
	throws EOFException, IOException {
		try {
			return queue.drainTo(buffer);
		} catch(final UnsupportedOperationException | IllegalArgumentException e) {
			throw new IOException(e);
		}
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
}
