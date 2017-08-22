package com.emc.mongoose.api.model.item;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.emc.mongoose.api.model.io.task.IoTask;

/**
 Created by kurila on 16.01.17.
 */
public interface ChainTransferBuffer<I extends Item, O extends IoTask<I>>
extends Input<I>, Output<O> {
}
