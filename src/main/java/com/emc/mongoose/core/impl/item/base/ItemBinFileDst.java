package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemFileDst;
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
public class ItemBinFileDst<T extends Item>
extends ItemBinDst<T>
implements ItemFileDst<T> {
	//
	protected final Path itemsDstPath;
	/**
	 @param itemsDstPath the path to the file which should be used to store the serialized items
	 @throws IOException if unable to open the file for writing
	 */
	public
	ItemBinFileDst(final Path itemsDstPath)
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
	public
	ItemBinFileDst()
	throws IOException {
		this(Files.createTempFile(null, ".bin"));
		this.itemsDstPath.toFile().deleteOnExit();
	}
	//
	@Override
	public
	ItemBinFileSrc<T> getItemSrc()
	throws IOException {
		return new ItemBinFileSrc<>(itemsDstPath);
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
