package com.emc.mongoose.core.api.persist;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
//
import java.io.Closeable;
/**
 Created by kurila on 14.11.14.
 */
public interface DataItemBuffer<T extends DataItem>
extends Consumer<T>, Producer<T>, Closeable {
	String FMT_THREAD_NAME = "%sDataItemsBuffer";
}
