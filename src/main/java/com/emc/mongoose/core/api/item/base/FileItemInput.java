package com.emc.mongoose.core.api.item.base;
//
import com.emc.mongoose.common.io.Input;

import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 20.10.15.
 */
public interface FileItemInput<T extends Item>
extends Input<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
