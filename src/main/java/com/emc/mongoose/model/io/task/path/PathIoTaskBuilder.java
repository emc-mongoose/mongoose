package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.PathItem;

/**
 Created by andrey on 31.01.17.
 */
public interface PathIoTaskBuilder<I extends PathItem, O extends PathIoTask<I>>
extends IoTaskBuilder<I, O> {
}
