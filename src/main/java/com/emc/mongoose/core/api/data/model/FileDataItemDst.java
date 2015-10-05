package com.emc.mongoose.core.api.data.model;
import com.emc.mongoose.core.api.data.DataItem;

import java.io.IOException;
import java.nio.file.Path;
/**
 Created by kurila on 11.08.15.
 */
public interface FileDataItemDst<T extends DataItem>
extends DataItemDst<T> {
	//
	Path getFilePath();
	//
	void delete()
	throws IOException;
}
