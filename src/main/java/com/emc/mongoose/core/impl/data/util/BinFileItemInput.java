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
 Created by kurila on 18.06.15.
 */
public class BinFileItemInput<T extends DataItem>
extends DeserializingItemInput<T> {
	//
	protected final Path itemsSrcPath;
	//
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
