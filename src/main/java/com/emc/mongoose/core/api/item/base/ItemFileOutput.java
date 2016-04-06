package com.emc.mongoose.core.api.item.base;
//
import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 11.08.15.
 */
public interface ItemFileOutput<T extends Item>
extends Output<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
