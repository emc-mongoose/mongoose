package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
//
import java.util.Queue;
/**
 Created by kurila on 09.10.14.
 */
public interface StorageNodeExecutor<T extends DataItem>
extends LoadExecutor<T> {
	//
	String getAddr();
	//
	boolean isShutdown();
	//
	Queue getQueue();
	//
	int getActiveCount();
	//
	void logMetrics(final Level level, final Marker marker);
	//
	void run();
	//
	@Override
	void interrupt(); // overridden to throw nothing
	//
	@Override
	void submit(final T dataItem); // overridden to throw nothing
	//
	@Override
	void setConsumer(final Consumer<T> consumer); // overridden to throw nothing
}
