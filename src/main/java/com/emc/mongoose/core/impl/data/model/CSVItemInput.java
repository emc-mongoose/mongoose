package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
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
public class CSVItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected BufferedReader itemsSrc;
	protected final Constructor<? extends T> itemConstructor;
	private DataItem lastItem = null;
	//
	private static final Logger LOG = LogManager.getLogger();
	/**
	 @param in the input stream to read the data item records from
	 @param itemCls the particular data item implementation class used to parse the records
	 @throws IOException
	 @throws NoSuchMethodException
	 */
	public CSVItemInput(final InputStream in, final Class<? extends T> itemCls)
	throws IOException, NoSuchMethodException {
		this(
			new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)),
			itemCls.getConstructor(String.class)
		);
	}
	//
	public CSVItemInput(
		final BufferedReader itemsSrc, final Constructor<? extends T> itemConstructor
	) {
		this.itemsSrc = itemsSrc;
		this.itemConstructor = itemConstructor;
	}
	//
	public void setItemsSrc(final BufferedReader itemsSrc) {
		this.itemsSrc = itemsSrc;
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
		for (int i = 0; i < itemsCount; i++) {
			if (itemsSrc.readLine() == null) {
				throw new IOException("Couldn't skip such amount of data items");
			}
		}
		LOG.info(Markers.MSG, DataItemInput.MSG_SKIP_END);
	}
	//
	@Override
	public T read()
	throws IOException {
		final String nextLine = itemsSrc.readLine();
		T nextItem = null;
		if(nextLine != null) {
			try {
				nextItem = itemConstructor.newInstance(nextLine);
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				throw new IOException(e);
			}
		}
		return nextItem;
	}
	//
	@Override
	public int read(final List<T> buffer, final int maxCount)
	throws IOException {
		int i;
		String nextLine;
		try {
			for(i = 0; i < maxCount; i ++) {
				nextLine = itemsSrc.readLine();
				if(nextLine == null) {
					if(i == 0) {
						throw new EOFException();
					} else {
						break;
					}
				}
				buffer.add(itemConstructor.newInstance(nextLine));
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
