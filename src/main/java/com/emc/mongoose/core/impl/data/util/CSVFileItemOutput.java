package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.util.DataItemOutput;
//
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 The data item output writing into the specified file human-readable data item records using the CSV
 format
 */
public class CSVFileItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final Path itemsDstPath;
	protected final Class<T> itemCls;
	protected final BufferedWriter itemsDst;
	//
	public CSVFileItemOutput(final Path itemsDstPath, final Class<T> itemCls)
	throws IOException {
		this.itemsDstPath = itemsDstPath;
		this.itemCls = itemCls;
		itemsDst = Files.newBufferedWriter(
			itemsDstPath, StandardCharsets.UTF_8,
			StandardOpenOption.APPEND, StandardOpenOption.WRITE
		);
	}
	//
	@Override
	public void write(final T dataItem)
	throws IOException {
		itemsDst.write(dataItem.toString());
		itemsDst.newLine();
	}
	//
	@Override
	public CSVFileItemInput<T> getInput()
	throws IOException {
		try {
			return new CSVFileItemInput<>(itemsDstPath, itemCls);
		} catch(final NoSuchMethodException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsDst.close();
	}
}
