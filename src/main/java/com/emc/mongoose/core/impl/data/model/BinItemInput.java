package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 The data item input implementation deserializing the data items from the specified stream
 */
public class BinItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	protected final ObjectInputStream itemsSrc;
	protected List<T> remainingItems = null;
	protected String lastItemId = null;
	//
	public BinItemInput(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public T read()
	throws IOException {
		try {
			return (T) itemsSrc.readUnshared();
		} catch(final ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public void setLastItemId(final String lastItemId) {
		this.lastItemId = lastItemId;
	}
	//
	@Override
	public String getLastItemId() {
		return lastItemId;
	}
	//
	@Override
	public int read(final List<T> buffer, final int maxCount)
	throws IOException {
		int done = 0;
		//
		if(remainingItems == null) { // there are no remaining items, read new ones from the stream
			try {
				final Object o = itemsSrc.readUnshared();
				if(DataItem.class.isInstance(o)) { // there are single item read from the stream
					if(maxCount > 0) {
						buffer.add((T) o);
						done = 1;
					}
				} else if(List.class.isInstance(o)) { // there are a list of items have been read
					final List<T> l = (List<T>) o;
					final int countAvail = l.size();
					if(countAvail <= maxCount) { // list of read items fits the buffer limit
						buffer.addAll(l);
						done = countAvail;
					} else { // list of read items doesn't fit the buffer limit
						buffer.addAll(l.subList(0, maxCount));
						remainingItems = l.subList(maxCount, countAvail);
						done = maxCount;
					}
				}
			} catch(final ClassNotFoundException | ClassCastException e) {
				throw new IOException(e);
			}
		} else {
			// do not read actually anything until all the remaining items are
			final int countRemaining = remainingItems.size();
			if(countRemaining <= maxCount) { // remaining items count fits the buffer limit
				buffer.addAll(remainingItems);
				done = countRemaining;
			} else { // remaining items count doesn't fit the buffer limit
				buffer.addAll(remainingItems.subList(0, maxCount));
				remainingItems = remainingItems.subList(maxCount, countRemaining);
				done = maxCount;
			}
		}
		//
		return done;
	}
	//
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
	}
	//
	@Override
	public void skip(final long countOfItems)
	throws IOException {
		LOG.info(Markers.MSG, "Attempt to skip processed data items. Wait for some time");
		for (int i = 0; i < countOfItems; i++) {
			try {
				itemsSrc.readUnshared();
			} catch (final ClassNotFoundException e) {
				throw new IOException(e);
			}
		}
		LOG.info(Markers.MSG, "Items were skipped successfully");
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsSrc.close();
	}
	//
	@Override
	public String toString() {
		return "binItemInput<" + itemsSrc.toString() + ">";
	}
}
