package com.emc.mongoose.common.io.bin.file;

import com.emc.mongoose.common.io.Input;

import java.nio.file.Path;

/**
 Created by kurila on 20.10.15.
 */
public interface FileItemInput<T>
extends Input<T> {
	Path getFilePath();
}
