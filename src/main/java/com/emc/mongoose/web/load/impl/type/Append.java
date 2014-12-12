package com.emc.mongoose.web.load.impl.type;
//
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.impl.WSLoadExecutorBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.util.concurrent.ThreadLocalRandom;
//
/**
 Created by kurila on 23.09.14.
 */
public class Append<T extends WSObject>
extends WSLoadExecutorBase<T>
implements WSLoadExecutor<T> {
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
		throws RemoteException, InterruptedException {
		if(dataItem!=null) {
			final long appendSize = ThreadLocalRandom
				.current()
				.nextLong(minAppendSize, maxAppendSize + 1);
			dataItem.append(appendSize);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Append the object \"{}\": +{}",
					dataItem, RunTimeConfig.formatSize(appendSize)
				);
			}
		}
		super.submit(dataItem);
	}
}
