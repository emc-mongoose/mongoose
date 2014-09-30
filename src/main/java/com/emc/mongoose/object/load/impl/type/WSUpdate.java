package com.emc.mongoose.object.load.impl.type;
//
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.impl.WSLoadExecutor;
import com.emc.mongoose.object.data.WSDataObject;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 06.05.14.
 */
public class WSUpdate<T extends WSDataObject>
extends WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int updatesPerObject;
	//
	public WSUpdate(
		final String[] addrs, final WSObjectRequestConfig<T> sharedReqConf, final long maxCount,
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
