package com.emc.mongoose.base.data.persist;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
//
import java.io.Closeable;
import java.io.Externalizable;
/**
 Created by kurila on 14.11.14.
 */
public interface DataItemBuffer<T extends DataItem>
extends Consumer<T>, Producer<T>, Closeable, Externalizable {
	String FMT_THREAD_NAME = "%sDataItemsBuffer-%s-";
}
