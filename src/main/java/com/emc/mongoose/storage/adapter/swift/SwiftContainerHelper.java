package com.emc.mongoose.storage.adapter.swift;
//
//
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
import com.emc.mongoose.core.api.v1.item.data.ContainerHelper;
/**
 Created by kurila on 02.03.15.
 */
public interface SwiftContainerHelper<T extends HttpDataItem, C extends Container<T>>
extends ContainerHelper<T, C> {}
