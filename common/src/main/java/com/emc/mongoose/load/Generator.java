package com.emc.mongoose.load;

import com.emc.mongoose.concurrent.LifeCycle;
import com.emc.mongoose.io.IoTask;
import com.emc.mongoose.item.Item;

/**
 Created on 11.07.16.
 */
public interface Generator<I extends Item, O extends IoTask<I>>
extends LifeCycle {
	
}
