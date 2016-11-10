package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.bin.file.FileItemOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 Created by kurila on 30.06.15.
 */
public class CsvFileItemOutput<I extends Item>
extends CsvItemOutput<I>
implements FileItemOutput<I> {
	
	protected Path itemsFilePath;
	
	public CsvFileItemOutput(final Path itemsFilePath, final ItemFactory<I> itemFactory)
	throws IOException {
		super(
			Files.newOutputStream(
				itemsFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE
			),
			itemFactory
		);
		this.itemsFilePath = itemsFilePath;
	}
	
	public CsvFileItemOutput(final ItemFactory<I> itemFactory)
	throws IOException {
		this(Files.createTempFile(null, ".csv"), itemFactory);
		this.itemsFilePath.toFile().deleteOnExit();
	}
	
	@Override
	public CsvFileItemInput<I> getInput()
	throws IOException {
		try {
			return new CsvFileItemInput<>(itemsFilePath, itemFactory);
		} catch(final NoSuchMethodException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public String toString() {
		return "csvFileItemOutput<" + itemsFilePath.getFileName() + ">";
	}
	
	@Override
	public final Path getFilePath() {
		return itemsFilePath;
	}
	
	@Override
	public final void delete()
	throws IOException {
		Files.delete(itemsFilePath);
	}
}
