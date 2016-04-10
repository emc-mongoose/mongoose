package com.emc.mongoose.core.impl.v1.io.task;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.core.api.v1.item.data.MutableDataItem;
import com.emc.mongoose.core.api.v1.io.conf.IoConfig;
import com.emc.mongoose.core.api.v1.io.task.DataIoTask;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
/**
 Created by andrey on 12.10.14.
 */
public class BasicDataIoTask<
	T extends MutableDataItem, C extends Container<T>, X extends IoConfig<T, C>
> extends BasicIoTask<T, C, X>
implements DataIoTask<T> {
	//
	protected final long contentSize;
	protected volatile long countBytesSkipped = 0;
	protected volatile DataItem currRange = null;
	protected volatile long currRangeSize = 0, nextRangeOffset = 0;
	protected volatile int currRangeIdx = 0, currDataLayerIdx = 0;
	//
	public BasicDataIoTask(final T item, final String nodeAddr, final X ioConfig) {
		super(item, nodeAddr, ioConfig);
		item.reset();
		currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case WRITE:
				if(item.hasScheduledUpdates()) {
					contentSize = item.getUpdatingRangesSize();
				} else if(item.isAppending()) {
					contentSize = item.getAppendSize();
				} else {
					contentSize = item.getSize();
				}
				break;
			case READ:
				// TODO partial content support
				contentSize = item.getSize();
				break;
			case DELETE:
				contentSize = 0;
				break;
			default:
				contentSize = 0;
				break;
		}
	}
}
