package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.item.data.FileItem;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedFileLoadSvc<F extends FileItem>
extends FileLoadSvc<F>, MixedDataLoadSvc<F> {
}
