package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.data.model.DataItemOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
/**
 The blocking queue wrapped in order to act as data item output from the tail and as data item input
 from the head.
 */
public class ItemBlockingQueue<T extends DataItem>
implements DataItemOutput<T>, DataItemInput<T> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private DataItem lastItem = null;
	protected final BlockingQueue<T> queue;
	//
	public ItemBlockingQueue(final BlockingQueue<T> queue) {
		this.queue = queue;
	}
	/**
	 Blocking write implementation
	 @param dataItem the data item to write
	 @throws InterruptedIOException if the thread was interrupted while trying to write
	 */
	@Override
	public void write(final T dataItem)
	throws InterruptedIOException {
		try {
			queue.put(dataItem);
		} catch(final InterruptedException e) {
			throw new InterruptedIOException();
		}
	}
	/**
	 Non-blocking bulk write implementation, THE RESULTING COUNT IS NOT THREAD SAFE
	 @param buffer the buffer containing the data items to write
	 @return the count of the items been written
	 @throws IOException doesn't throw
	 */
	@Override
	public int write(final List<T> buffer)
	throws IOException {
		final int n = queue.size();
		try {
			queue.addAll(buffer);
		} catch(final IllegalStateException e) {
		}
		return queue.size() - n;
	}
	/**
	 @return self
	 @throws IOException doesn't throw
	 */
	@Override
	public ItemBlockingQueue<T> getInput()
	throws IOException {
		return this;
	}
	/**
	 Blocking read implementation
	 @return the data item
	 @throws InterruptedIOException if the thread was interrupted while trying to read
	 */
	@Override
	public T read()
	throws InterruptedIOException {
		try {
			return queue.take();
		} catch(final InterruptedException e) {
			throw new InterruptedIOException();
		}
	}
	/**
	 Non-blocking bulk read implementation
	 @param maxCount the count limit
	 @param buffer buffer for the data items
	 @return the count of the items been read
	 @throws IOException if something goes wrong
	 */
	@Override
	public int read(final List<T> buffer, final int maxCount)
	throws IOException {
		try {
			return queue.drainTo(buffer, maxCount);
		} catch(final UnsupportedOperationException | IllegalArgumentException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public DataItem getLastDataItem() {
		return lastItem;
	}
	//
	@Override
	public void setLastDataItem(final T lastItem) {
		this.lastItem = lastItem;
	}
	//
	@Override
	public void skip(final long itemsCount)
	throws IOException {
		LOG.info(Markers.MSG, DataItemInput.MSG_SKIP_START, itemsCount);
		try {
			T item;
			for (int i = 0; i < itemsCount; i++) {
				item = queue.take();
				if (item.equals(lastItem)) {
					return;
				}
			}
		} catch (final InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
		LOG.info(Markers.MSG, DataItemInput.MSG_SKIP_END);
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
	public final String toString() {
		return "itemsQueue#" + hashCode() + "";
	}
}
