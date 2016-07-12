package com.emc.mongoose.common.load;

import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;

import java.io.Closeable;

/**
 Created on 11.07.16.
 */
public interface Generator<I extends Item, O extends IoTask<I>>
extends Closeable, LifeCycle {
}
