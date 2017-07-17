package com.emc.mongoose.api.common.io.bin.file;

import com.emc.mongoose.api.common.io.Output;

import java.nio.file.Path;

/**
 Created by kurila on 11.08.15.
 */
public interface FileItemOutput<T>
extends Output<T> {
	Path getFilePath();
}
