package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.Item;
//
import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 20.10.15.
 */
public interface FileItemSrc<T extends Item>
extends ItemSrc<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
