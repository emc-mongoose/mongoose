package com.emc.mongoose.object.load.type.ws;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.WSLoadExecutorBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 23.09.14.
 */
public class Append<T extends WSObjectImpl>
extends WSLoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final long minAppendSize, maxAppendSize;
	//
	public Append(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minAppendSize, final long maxAppendSize
	) {
		super(runTimeConfig, addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
		this.minAppendSize = minAppendSize;
		this.maxAppendSize = maxAppendSize;
	}
	//
	@Override
	public void submit(final T dataItem)
	throws RemoteException {
		if(dataItem!=null) {
			final long appendSize = ThreadLocalRandom
				.current()
				.nextLong(minAppendSize, maxAppendSize + 1);
			dataItem.append(appendSize);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Append the object \"{}\": +{}",
					dataItem.getId(), RunTimeConfig.formatSize(appendSize)
				);
			}
		}
		super.submit(dataItem);
	}
	//
}
