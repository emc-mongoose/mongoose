package com.emc.mongoose.item;

import com.emc.mongoose.io.Output;

import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 11.08.15.
 */
public interface FileItemOutput<T extends Item>
extends Output<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
