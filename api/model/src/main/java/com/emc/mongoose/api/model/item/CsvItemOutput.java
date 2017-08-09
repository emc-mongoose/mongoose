package com.emc.mongoose.api.model.item;

import com.emc.mongoose.api.common.Constants;
import com.emc.mongoose.api.common.io.Output;

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
		itemsDst = new BufferedWriter(
			new OutputStreamWriter(out, StandardCharsets.UTF_8), Constants.MIB
		);
		this.itemFactory = itemFactory;
	}
	//
	@Override
	public boolean put(final I item)
	throws IOException {
		itemsDst.write(item.toString());
		itemsDst.newLine();
		return true;
	}
	//
	@Override
	public int put(final List<I> buffer, final int from, final int to)
	throws IOException {
		int i = from;
		while(i < to && put(buffer.get(i))) {
			i ++;
		}
		return i - from;
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
		itemFactory.close();
	}
	//
	@Override
	public String toString() {
		return "csvItemOutput<" + itemsDst + ">";
	}
}
