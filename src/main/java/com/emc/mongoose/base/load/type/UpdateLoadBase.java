package com.emc.mongoose.base.load.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.UpdatableDataItem;
import com.emc.mongoose.base.load.impl.LoadExecutorBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 20.10.14.
 */
public abstract class UpdateLoadBase<T extends UpdatableDataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int updatesPerObject;
	//
	protected UpdateLoadBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final RequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile, final int updatesPerObject
	) {
		super(runTimeConfig, addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
		this.updatesPerObject = updatesPerObject;
	}
	//
	@Override
	public final void submit(final T dataItem)
	throws RemoteException, InterruptedException {
		if(dataItem!=null) {
			try {
				dataItem.updateRandomRanges(updatesPerObject);
			} catch(final Exception e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to create modified ranges");
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Modified {} ranges for object \"{}\"", updatesPerObject, dataItem
				);
			}
		}
		super.submit(dataItem);
	}
}
