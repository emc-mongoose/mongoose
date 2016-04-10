package com.emc.mongoose.core.api.v1.io.conf;
//
import com.emc.mongoose.core.api.v1.item.container.Directory;
import com.emc.mongoose.core.api.v1.item.data.FileItem;
/**
 Created by kurila on 27.11.15.
 */
public interface FileIoConfig<F extends FileItem, D extends Directory<F>>
extends IoConfig<F, D> {
	String getTargetItemPath();
}
