package com.emc.mongoose.core.impl.io.task;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.DataIOTask;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
/**
 Created by andrey on 12.10.14.
 */
public class BasicDataIOTask<
	T extends MutableDataItem, C extends Container<T>, X extends IOConfig<T, C>
> extends BasicIOTask<T, C, X>
implements DataIOTask<T> {
	//
	protected final long contentSize;
	protected volatile long countBytesSkipped = 0;
	protected volatile DataItem currRange = null;
	protected volatile long currRangeSize = 0, nextRangeOffset = 0;
	protected volatile int currRangeIdx = 0, currDataLayerIdx = 0;
	//
	public BasicDataIOTask(final T item, final String nodeAddr, final X ioConfig) {
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
