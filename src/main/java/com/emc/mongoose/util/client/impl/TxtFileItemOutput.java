package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.util.client.api.DataItemOutput;
//
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 18.06.15.
 */
public class TxtFileItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final Path itemsDstPath;
	protected final BufferedWriter itemsDst;
	//
	public TxtFileItemOutput(final Path itemsDstPath)
	throws IOException {
		this.itemsDstPath = itemsDstPath;
		itemsDst = Files.newBufferedWriter(
			itemsDstPath, StandardCharsets.UTF_8,
			StandardOpenOption.APPEND, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
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
	public TxtFileItemInput<T> getInput()
	throws IOException {
		return new TxtFileItemInput<>(itemsDstPath);
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsDst.close();
	}
}
