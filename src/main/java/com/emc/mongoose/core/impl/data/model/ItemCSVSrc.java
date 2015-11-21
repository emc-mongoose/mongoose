package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.data.model.ItemSrc;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 The data item input using CSV file containing the human-readable data item records as the source
 */
public class ItemCSVSrc<T extends Item>
implements ItemSrc<T> {
	//
	protected BufferedReader itemsSrc;
	protected final Constructor<? extends T> itemConstructor;
	protected final ContentSource contentSrc;
	private T lastItem = null;
	//
	private static final Logger LOG = LogManager.getLogger();
	/**
	 @param in the input stream to get the data item records from
	 @param itemCls the particular data item implementation class used to parse the records
	 @throws IOException
	 @throws NoSuchMethodException
	 */
	public
	ItemCSVSrc(
		final InputStream in, final Class<? extends T> itemCls, final ContentSource contentSrc
	) throws IOException, NoSuchMethodException {
		this(
			new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)),
			itemCls.getConstructor(String.class, ContentSource.class), contentSrc
		);
	}
	//
	protected
	ItemCSVSrc(
		final BufferedReader itemsSrc, final Constructor<? extends T> itemConstructor,
		final ContentSource contentSrc
	) {
		this.itemsSrc = itemsSrc;
		this.itemConstructor = itemConstructor;
		this.contentSrc = contentSrc;
	}
	//
	public void setItemsSrc(final BufferedReader itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	@Override
	public T getLastItem() {
		return lastItem;
	}
	//
	@Override
	public void setLastItem(final T lastItem) {
		this.lastItem = lastItem;
	}
	//
	@Override
	public void skip(final long itemsCount)
	throws IOException {
		LOG.info(Markers.MSG, ItemSrc.MSG_SKIP_START, itemsCount);
		String item;
		for (int i = 0; i < itemsCount; i++) {
			item = itemsSrc.readLine();
			if (item == null) {
				throw new IOException("Couldn't skip such amount of data items");
			} else if (item.equals(lastItem.toString())) {
				LOG.info(Markers.MSG, ItemSrc.MSG_SKIP_END);
				return;
			}
		}
	}
	//
	@Override
	public T get()
	throws IOException {
		final String nextLine = itemsSrc.readLine();
		try {
			return nextLine == null ? null : itemConstructor.newInstance(nextLine, contentSrc);
		} catch(
			final InstantiationException | IllegalAccessException | InvocationTargetException e
		) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public int get(final List<T> buffer, final int limit)
	throws IOException {
		int i;
		String nextLine;
		try {
			for(i = 0; i < limit; i ++) {
				nextLine = itemsSrc.readLine();
				if(nextLine == null) {
					if(i == 0) {
						throw new EOFException();
					} else {
						break;
					}
				}
				buffer.add(itemConstructor.newInstance(nextLine, contentSrc));
			}
		} catch(
			final InstantiationException | IllegalAccessException | InvocationTargetException e
		) {
			throw new IOException(e);
		}
		return i;
	}
	//
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
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
		return "csvItemInput<" + itemsSrc.toString() + ">";
	}
}
