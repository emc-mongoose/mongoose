package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.nio.file.Path;
/**
 Created by kurila on 11.08.15.
 */
public interface FileDataItemInput<T extends DataItem>
extends DataItemInput<T> {
	//
	Path getFilePath();
	//
	long getApproxDataItemsSize(final int maxCount);
}
