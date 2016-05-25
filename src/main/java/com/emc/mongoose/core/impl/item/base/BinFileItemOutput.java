package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.FileItemOutput;
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
public class BinFileItemOutput<T extends Item>
extends ItemBinOutput<T>
implements FileItemOutput<T> {
	//
	protected final Path itemsDstPath;
	/**
	 @param itemsDstPath the path to the file which should be used to store the serialized items
	 @throws IOException if unable to open the file for writing
	 */
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
	public BinFileItemOutput()
	throws IOException {
		this(Files.createTempFile(null, ".bin"));
		this.itemsDstPath.toFile().deleteOnExit();
	}
	//
	@Override
	public BinFileInput<T> getInput()
	throws IOException {
		return new BinFileInput<>(itemsDstPath);
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
	//
	@Override
	public final void delete()
	throws IOException {
		Files.delete(itemsDstPath);
	}
}
