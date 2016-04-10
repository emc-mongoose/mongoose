package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.load.IoTask;
/**
 Created by andrey on 08.04.16.
 */
public interface ContainerLoadExecutor<T extends Item, C extends Container<T>, A extends IoTask<C>>
extends LoadExecutor<C, A> {
}
