package com.emc.mongoose.model.item;

import com.emc.mongoose.common.Constants;
import com.emc.mongoose.common.io.Input;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 The data item input using CSV file containing the human-readable data item records as the source
 */
public class CsvItemInput<I extends Item>
implements Input<I> {
	//
	protected BufferedReader itemsSrc;
	protected final ItemFactory<I> itemFactory;
	private I lastItem = null;
	//
	/**
	 @param in the input stream to get the data item records from
	 @param itemFactory the concrete item factory used to parse the records
	 @throws IOException
	 @throws NoSuchMethodException
	 */
	public CsvItemInput(final InputStream in, final ItemFactory<I> itemFactory)
	throws IOException, NoSuchMethodException {
		this(
			new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), Constants.MIB),
			itemFactory
		);
	}
	//
	protected CsvItemInput(final BufferedReader itemsSrc, final ItemFactory<I> itemFactory) {
		this.itemsSrc = itemsSrc;
		this.itemFactory = itemFactory;
	}
	//
	public void setItemsSrc(final BufferedReader itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	@Override
	public void skip(final long itemsCount)
	throws IOException {
		String item;
		for(int i = 0; i < itemsCount; i++) {
			item = itemsSrc.readLine();
			if(item == null) {
				throw new IOException("Couldn't skip such amount of data items");
			} else if(item.equals(lastItem.toString())) {
				return;
			}
		}
	}
	//
	@Override
	public I get()
	throws IOException {
		final String nextLine = itemsSrc.readLine();
		return nextLine == null ? null : itemFactory.getItem(nextLine);
	}
	//
	@Override
	public int get(final List<I> buffer, final int limit)
	throws IOException {
		int i;
		String nextLine;
		for(i = 0; i < limit; i ++) {
			nextLine = itemsSrc.readLine();
			if(nextLine == null) {
				if(i == 0) {
					throw new EOFException();
				} else {
					break;
				}
			}
			buffer.add(itemFactory.getItem(nextLine));
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
