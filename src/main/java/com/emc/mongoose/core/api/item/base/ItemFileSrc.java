package com.emc.mongoose.core.api.item.base;
//
//
import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 20.10.15.
 */
public interface ItemFileSrc<T extends Item>
extends ItemSrc<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
