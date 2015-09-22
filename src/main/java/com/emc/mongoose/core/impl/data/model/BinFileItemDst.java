package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.FileDataItemDst;
//
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 An item input implementation serializing the data items to the specified file.
 */
public class BinFileItemDst<T extends DataItem>
extends BinItemDst<T>
implements FileDataItemDst<T> {
	//
	protected final Path itemsDstPath;
	/**
	 @param itemsDstPath the path to the file which should be used to store the serialized items
	 @throws IOException if unable to open the file for writing
	 */
	public BinFileItemDst(final Path itemsDstPath)
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
	public BinFileItemDst()
	throws IOException {
		this(Files.createTempFile(null, ".bin"));
		this.itemsDstPath.toFile().deleteOnExit();
	}
	//
	@Override
	public BinFileItemSrc<T> getDataItemSrc()
	throws IOException {
		return new BinFileItemSrc<>(itemsDstPath);
	}
	//
	@Override
	public String toString() {
		return "binFileItemOutput<" + itemsDstPath.getFileName() + ">";
	}
	//
	@Override
	public final Path getFilePath() {
		return itemsDstPath;
	}
}
