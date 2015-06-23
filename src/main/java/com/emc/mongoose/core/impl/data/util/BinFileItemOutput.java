package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 18.06.15.
 */
public class BinFileItemOutput<T extends DataItem>
extends SerializingItemOutput<T> {
	//
	protected final Path itemsDstPath;
	//
	public BinFileItemOutput(final Path itemsDstPath)
	throws IOException {
		super(
			new ObjectOutputStream(
				new BufferedOutputStream(
					Files.newOutputStream(
						itemsDstPath, StandardOpenOption.APPEND, StandardOpenOption.WRITE
					)
				)
			)
		);
		this.itemsDstPath = itemsDstPath;
	}
	//
	@Override
	public BinFileItemInput<T> getInput()
	throws IOException {
		return new BinFileItemInput<>(itemsDstPath);
	}
}
