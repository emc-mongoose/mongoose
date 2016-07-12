package com.emc.mongoose.common.item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 An item input implementation deserializing the data items from the specified file.
 */
public class BinFileItemInput<T extends Item>
extends BinItemInput<T>
implements FileItemInput<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Path itemsSrcPath;
	/**
	 @param itemsSrcPath the path to the file which should be used to restore the serialized items
	 @throws IOException if unable to open the file for reading
	 */
	public BinFileItemInput(final Path itemsSrcPath)
	throws IOException {
		super(
			buildObjectInputStream(itemsSrcPath)
		);
		this.itemsSrcPath = itemsSrcPath;
	}
	//
	protected static ObjectInputStream buildObjectInputStream(final Path itemsSrcPath)
	throws IOException {
		return new ObjectInputStream(
			new BufferedInputStream(
				Files.newInputStream(itemsSrcPath, StandardOpenOption.READ)
			)
		);
	}
	//
	@Override
	public String toString() {
		return "binFileItemInput<" + itemsSrcPath.getFileName() + ">";
	}
	//
	@Override
	public final Path getFilePath() {
		return itemsSrcPath;
	}
	//
	@Override
	public final void delete()
		throws IOException {
		Files.delete(itemsSrcPath);
	}
	//
	@Override
	public void reset()
	throws IOException {
		if (itemsSrc != null) {
			itemsSrc.close();
		}
		setItemsSrc(buildObjectInputStream(itemsSrcPath));
	}
}
