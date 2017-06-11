package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.io.task.IoTask;

/**
 Created by kurila on 16.01.17.
 */
public interface IoResultsOutputItemInput<I extends Item, O extends IoTask<I>>
extends Input<I>, Output<O> {
}
