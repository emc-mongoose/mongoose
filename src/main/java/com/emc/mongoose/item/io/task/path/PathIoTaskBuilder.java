package com.emc.mongoose.item.io.task.path;

import com.emc.mongoose.item.io.task.IoTaskBuilder;
import com.emc.mongoose.item.PathItem;

/**
 Created by andrey on 31.01.17.
 */
public interface PathIoTaskBuilder<I extends PathItem, O extends PathIoTask<I>>
extends IoTaskBuilder<I, O> {
}
