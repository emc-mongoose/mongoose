package com.emc.mongoose.item.io.task.token;

import com.emc.mongoose.item.io.task.IoTaskBuilder;
import com.emc.mongoose.item.TokenItem;

/**
 Created by kurila on 14.07.16.
 */
public interface TokenIoTaskBuilder<I extends TokenItem, O extends TokenIoTask<I>>
extends IoTaskBuilder<I, O> {
}
