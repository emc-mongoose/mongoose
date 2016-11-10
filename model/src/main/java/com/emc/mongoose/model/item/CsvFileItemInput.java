package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.bin.file.FileItemInput;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.Files.newInputStream;

/**
 Created by kurila on 30.06.15.
 */
public class CsvFileItemInput<I extends Item>
extends CsvItemInput<I>
implements FileItemInput<I> {
	//
	protected final Path itemsFilePath;
	/**
	 @param itemsFilePath the input stream to get the data item records from
	 @param itemFactory the concrete item factory
	 @throws IOException
	 @throws NoSuchMethodException */
	public CsvFileItemInput(final Path itemsFilePath, final ItemFactory<I> itemFactory)
	throws IOException, NoSuchMethodException {
		super(newInputStream(itemsFilePath, StandardOpenOption.READ), itemFactory);
		this.itemsFilePath = itemsFilePath;
	}
	//
	@Override
	public String toString() {
		return "csvFileItemInput<" + itemsFilePath.getFileName() + ">";
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
	//
	@Override
	public void reset()
	throws IOException {
		if(itemsSrc != null) {
			itemsSrc.close();
		}
		setItemsSrc(Files.newBufferedReader(itemsFilePath, StandardCharsets.UTF_8));
	}
}
