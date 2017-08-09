package com.emc.mongoose.api.model.io.task.token;

import com.emc.mongoose.api.model.io.task.IoTaskBuilder;
import com.emc.mongoose.api.model.item.TokenItem;

/**
 Created by kurila on 14.07.16.
 */
public interface TokenIoTaskBuilder<I extends TokenItem, O extends TokenIoTask<I>>
extends IoTaskBuilder<I, O> {
}
