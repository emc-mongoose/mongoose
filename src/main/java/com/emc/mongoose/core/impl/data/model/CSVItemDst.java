package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.data.model.ItemDst;
//
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
public abstract class CSVItemDst<T extends DataItem>
implements ItemDst<T> {
	//
	protected final Class<? extends T> itemCls;
	protected final ContentSource contentSrc;
	protected final BufferedWriter itemsDst;
	//
	protected CSVItemDst(
		final OutputStream out, final Class<? extends T> itemCls, final ContentSource contentSrc
	) throws IOException {
		itemsDst = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		this.itemCls = itemCls;
		this.contentSrc = contentSrc;
	}
	//
	@Override
	public void put(final T dataItem)
	throws IOException {
		itemsDst.write(dataItem.toString());
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
