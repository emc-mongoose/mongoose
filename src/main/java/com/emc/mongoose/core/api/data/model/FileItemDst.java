package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.Item;

import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 11.08.15.
 */
public interface FileItemDst<T extends Item>
extends ItemDst<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
