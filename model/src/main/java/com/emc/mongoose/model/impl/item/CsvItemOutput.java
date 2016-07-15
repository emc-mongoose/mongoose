package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.item.Item;

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
public abstract class CsvItemOutput<T extends Item>
implements Output<T> {
	//
	protected final Class<? extends T> itemCls;
	protected final ContentSource contentSrc;
	protected final BufferedWriter itemsDst;
	//
	protected CsvItemOutput(
		final OutputStream out, final Class<? extends T> itemCls, final ContentSource contentSrc
	) throws IOException {
		itemsDst = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		this.itemCls = itemCls;
		this.contentSrc = contentSrc;
	}
	//
	@Override
	public void put(final T item)
	throws IOException {
		itemsDst.write(item.toString());
		itemsDst.newLine();
	}
	//
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		for(int i = from; i < to; i ++) {
			put(buffer.get(i));
		}
		return to - from;
	}
	//
	@Override
	public final int put(final List<T> items)
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
