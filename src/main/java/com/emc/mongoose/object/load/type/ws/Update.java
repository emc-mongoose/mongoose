package com.emc.mongoose.object.load.type.ws;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.WSLoadExecutorBase;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 06.05.14.
 */
public class Update<T extends WSObjectImpl>
extends WSLoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int updatesPerObject;
	//
	public Update(
		final String[] addrs, final WSRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile, final int updatesPerObject
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
		this.updatesPerObject = updatesPerObject;
	}
	//
	@Override
	public final void submit(final T dataItem) {
		if(dataItem!=null) {
			try {
				dataItem.updateRandomRanges(updatesPerObject);
			} catch(final Exception e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to create modified ranges");
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Modified {}/{} ranges for object \"{}\"",
					dataItem.getPendingUpdatesCount(), updatesPerObject,
					Long.toHexString(dataItem.getId())
				);
			}
		}
		super.submit(dataItem);
	}
}
