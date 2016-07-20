package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.base.FileItemOutput;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 30.06.15.
 */
public class CsvFileItemOutput<T extends Item>
extends CsvItemOutput<T>
implements FileItemOutput<T> {
	//
	protected Path itemsFilePath;
	//
	public CsvFileItemOutput(
		final Path itemsFilePath, final Class<? extends T> itemCls, final ContentSource contentSrc
	) throws IOException {
		super(
			Files.newOutputStream(
				itemsFilePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE
			),
			itemCls, contentSrc
		);
		this.itemsFilePath = itemsFilePath;
	}
	//
	public CsvFileItemOutput(final Class<? extends T> itemCls, final ContentSource contentSrc)
	throws IOException {
		this(Files.createTempFile(null, ".csv"), itemCls, contentSrc);
		this.itemsFilePath.toFile().deleteOnExit();
	}
	//
	@Override
	public CsvFileItemInput<T> getInput()
	throws IOException {
		try {
			return new CsvFileItemInput<>(itemsFilePath, itemCls, contentSrc);
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
	//
	@Override
	public final void delete()
	throws IOException {
		Files.delete(itemsFilePath);
	}
}
