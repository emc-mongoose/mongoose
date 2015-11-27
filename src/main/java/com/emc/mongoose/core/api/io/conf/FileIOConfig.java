package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
/**
 Created by kurila on 27.11.15.
 */
public interface FileIOConfig<F extends FileItem, D extends Directory<F>>
extends IOConfig<F, D> {
}
