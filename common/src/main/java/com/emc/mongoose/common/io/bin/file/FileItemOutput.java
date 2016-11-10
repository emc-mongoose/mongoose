package com.emc.mongoose.common.io.bin.file;

import com.emc.mongoose.common.io.Output;

import java.io.IOException;
import java.nio.file.Path;

/**
 Created by kurila on 11.08.15.
 */
public interface FileItemOutput<T>
extends Output<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
