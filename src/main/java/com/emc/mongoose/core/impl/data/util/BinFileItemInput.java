package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 An item input implementation deserializing the data items from the specified file.
 */
public class BinFileItemInput<T extends DataItem>
extends ExternItemInput<T> {
	//
	protected final Path itemsSrcPath;
	/**
	 @param itemsSrcPath the path to the file which should be used to restore the serialized items
	 @throws IOException if unable to open the file for reading
	 */
	public BinFileItemInput(final Path itemsSrcPath)
	throws IOException {
		super(
			new ObjectInputStream(
				new BufferedInputStream(
					Files.newInputStream(itemsSrcPath, StandardOpenOption.READ)
				)
			)
		);
		this.itemsSrcPath = itemsSrcPath;
	}
}
