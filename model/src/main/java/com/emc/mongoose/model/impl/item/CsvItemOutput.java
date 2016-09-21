package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
/**
 The data item output writing into the specified file human-readable data item records using the CSV
 format
 */
public abstract class CsvItemOutput<I extends Item>
implements Output<I> {
	//
	protected final ItemFactory<I> itemFactory;
	protected final BufferedWriter itemsDst;
	//
	protected CsvItemOutput(final OutputStream out, final ItemFactory<I> itemFactory)
	throws IOException {
		itemsDst = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		this.itemFactory = itemFactory;
	}
	//
	@Override
	public void put(final I item)
	throws IOException {
		itemsDst.write(item.toString());
		itemsDst.newLine();
	}
	//
	@Override
	public int put(final List<I> buffer, final int from, final int to)
	throws IOException {
		for(int i = from; i < to; i ++) {
			put(buffer.get(i));
		}
		return to - from;
	}
	//
	@Override
	public final int put(final List<I> items)
	throws IOException {
		return put(items, 0, items.size());
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsDst.close();
	}
	//
	@Override
	public String toString() {
		return "csvItemOutput<" + itemsDst + ">";
	}
}
