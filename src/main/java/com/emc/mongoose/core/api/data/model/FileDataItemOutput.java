package com.emc.mongoose.core.api.data.model;
import com.emc.mongoose.core.api.data.DataItem;

import java.nio.file.Path;
/**
 Created by kurila on 11.08.15.
 */
public interface FileDataItemOutput<T extends DataItem>
extends DataItemOutput<T> {
	Path getFilePath();
}
