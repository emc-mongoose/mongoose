package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
/**
 Created by kurila on 27.11.15.
 */
public interface FileIoConfig<F extends FileItem, D extends Directory<F>>
extends IoConfig<F, D> {
	String getDstItemPath();
}
