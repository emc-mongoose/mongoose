package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.FileDataItemOutput;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 30.06.15.
 */
public class CSVFileItemOutput<T extends DataItem>
extends CSVItemOutput<T>
implements FileDataItemOutput<T> {
	//
	protected Path itemsFilePath;
	//
	public CSVFileItemOutput(final Path itemsFilePath, final Class<? extends T> itemCls)
	throws IOException {
		super(
			Files.newOutputStream(itemsFilePath, StandardOpenOption.WRITE),
			itemCls
		);
		this.itemsFilePath = itemsFilePath;
	}
	//
	public CSVFileItemOutput(final Class<? extends T> itemCls)
	throws IOException, NoSuchMethodException {
		this(Files.createTempFile(null, ".csv"), itemCls);
		this.itemsFilePath.toFile().deleteOnExit();
	}
	//
	@Override
	public CSVFileItemInput<T> getInput()
	throws IOException {
		try {
			return new CSVFileItemInput<>(itemsFilePath, itemCls);
		} catch(final NoSuchMethodException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public String toString() {
		return "csvFileItemOutput<" + itemsFilePath.getFileName() + ">";
	}
	//
	@Override
	public final Path getFilePath() {
		return itemsFilePath;
	}
}
