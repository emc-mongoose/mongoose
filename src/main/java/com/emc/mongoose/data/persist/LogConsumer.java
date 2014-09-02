package com.emc.mongoose.data.persist;
//
import com.emc.mongoose.Consumer;
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.logging.Markers;
//
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 Created by kurila on 12.05.14.
 */
@ThreadSafe
public class LogConsumer<T extends UniformData>
implements Consumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private long maxCount = Long.MAX_VALUE, count = 0;
	//
	public LogConsumer(final UniformDataSource dataSrc) {
		LOG.info(Markers.DATA_LIST, dataSrc.toString()); // header w/ data ring meta info
	}
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
		LOG.debug(Markers.MSG, "invoking close() here does nothing");
	}
	//
	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}
	//
}
