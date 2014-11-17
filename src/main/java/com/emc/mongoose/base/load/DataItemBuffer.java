package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
//
import java.io.Closeable;
import java.io.IOException;
/**
 Created by kurila on 14.11.14.
 */
public interface DataItemBuffer<T extends DataItem>
extends Consumer<T>, Producer<T>, Closeable {
	//
	String FMT_THREAD_NAME = "%sDataItemsBuffer";
	//
	public void join(final long milliSeconds)
	throws IOException, InterruptedException;
}
