package com.emc.mongoose.base.load.controller;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
/**
 Created by andrey on 30.09.14.
 */
public interface LoadBuilderClient<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
}
