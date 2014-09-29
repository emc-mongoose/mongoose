package com.emc.mongoose.object.load.impl.type;
//
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.impl.WSLoadExecutor;
import com.emc.mongoose.object.data.WSDataObject;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 23.09.14.
 */
public class WSAppend<T extends WSDataObject>
extends WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final long minAppendSize, maxAppendSize;
	//
	public WSAppend(
		final String[] addrs, final WSObjectRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minAppendSize, final long maxAppendSize
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
		this.minAppendSize = minAppendSize;
		this.maxAppendSize = maxAppendSize;
	}
	//
	@Override
	public final void submit(final T dataItem) {
		if(dataItem!=null) {
			final long appendSize = ThreadLocalRandom
				.current()
				.nextLong(minAppendSize, maxAppendSize + 1);
			dataItem.append(appendSize);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Append the object \"{}\": +{}",
					Long.toHexString(dataItem.getId()), RunTimeConfig.formatSize(appendSize)
				);
			}
		}
		super.submit(dataItem);
	}
	//
}
