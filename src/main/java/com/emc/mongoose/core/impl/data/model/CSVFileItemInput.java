package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 30.06.15.
 */
public class CSVFileItemInput<T extends DataItem>
extends CSVItemInput<T> {
	//
	protected final Path itemsFilePath;
	/**
	 @param itemsFilePath the input stream to read the data item records from
	 @param itemCls the particular data item implementation class used to parse the records
	 @throws java.io.IOException
	 @throws NoSuchMethodException */
	public CSVFileItemInput(final Path itemsFilePath, final Class<? extends T> itemCls)
	throws IOException, NoSuchMethodException {
		super(Files.newInputStream(itemsFilePath, StandardOpenOption.READ), itemCls);
		this.itemsFilePath = itemsFilePath;
	}
	//
	@Override
	public String toString() {
		return "csvFileItemInput<" + itemsFilePath.getFileName() + ">";
	}
}
