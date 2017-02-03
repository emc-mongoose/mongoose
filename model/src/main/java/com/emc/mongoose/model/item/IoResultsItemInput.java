package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.io.task.IoTask.IoResult;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 16.01.17.
 */
public interface IoResultsItemInput<I extends Item, R extends IoResult<I>>
extends Input<I>, Output<R> {
}
