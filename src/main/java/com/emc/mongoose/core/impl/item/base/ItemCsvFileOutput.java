package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.base.ItemFileOutput;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 30.06.15.
 */
public class ItemCsvFileOutput<T extends Item>
extends ItemCsvOutput<T>
implements ItemFileOutput<T> {
	//
	protected Path itemsFilePath;
	//
	public ItemCsvFileOutput(
		final Path itemsFilePath, final Class<? extends T> itemCls, final ContentSource contentSrc
	) throws IOException {
		super(
			Files.newOutputStream(
				itemsFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE
			),
			itemCls, contentSrc
		);
		this.itemsFilePath = itemsFilePath;
	}
	//
	public ItemCsvFileOutput(final Class<? extends T> itemCls, final ContentSource contentSrc)
	throws IOException {
		this(Files.createTempFile(null, ".csv"), itemCls, contentSrc);
		this.itemsFilePath.toFile().deleteOnExit();
	}
	//
	@Override
	public
	ItemCSVFileSrc<T> getInput()
	throws IOException {
		try {
			return new ItemCSVFileSrc<>(itemsFilePath, itemCls, contentSrc);
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
