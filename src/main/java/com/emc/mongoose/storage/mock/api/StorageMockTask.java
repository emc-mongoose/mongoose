package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.common.collections.Reusable;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.util.concurrent.Callable;
/**
 Created by andrey on 27.07.15.
 */
public interface StorageMockTask<T extends DataItem>
extends Reusable<StorageMockTask<T>>, Callable<T> {
}
