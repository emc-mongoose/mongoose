package com.emc.mongoose.base.data.persist;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 Created by kurila on 12.05.14.
 A consumer which simply redirects the data items to its logger.
 */
@ThreadSafe
public class LogConsumer<T extends DataItem>
implements Consumer<T> {
	//
	private static volatile Logger LOG = LogManager.getRootLogger();
	public static void setLogger(final Logger log) {
		LOG = log;
	}
	private long maxCount = Long.MAX_VALUE, count = 0;
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public final void setMaxCount(final long maxCount) {
		this.maxCount = maxCount;
	}
	//
	@Override
	public synchronized void submit(final T data) {
		if(data!=null && count < maxCount) {
			LOG.info(Markers.DATA_LIST, data.toString());
			count++;
		}
	}
	//
	@Override
	public final void close()
	throws IOException {
		LOG.trace(Markers.MSG, "invoking close() here does nothing");
	}
	//
	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}
	//
}
