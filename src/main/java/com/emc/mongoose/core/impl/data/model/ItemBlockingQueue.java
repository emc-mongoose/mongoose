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
	protected final BlockingQueue<T> queue;
	protected String lastItemId = null;
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
	@Override
	public void skip(final long countOfItems)
	throws IOException {
		LOG.info(Markers.MSG, "Attempt to skip processed data items. Wait for some time");
		for (int i = 0; i < countOfItems; i++) {
			try {
				queue.take();
			} catch (final InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
		LOG.info(Markers.MSG, "Data items were successfully skipped");
	}
	@Override
	public void setLastItemId(final String lastItemId) {
		this.lastItemId = lastItemId;
	}
	@Override
	public String getLastItemId() {
		return lastItemId;
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
		return "itemsQueue<" + queue.toString() + ">";
	}
}
