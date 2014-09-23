package com.emc.mongoose.object.http.impl;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.WSLoadExecutor;
import com.emc.mongoose.object.http.api.WSRequestConfig;
import com.emc.mongoose.object.http.data.WSObject;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 23.09.14.
 */
public class Append
extends WSLoadExecutor {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final long minAppendSize, maxAppendSize;
	//
	public Append(
		final String[] addrs, final WSRequestConfig sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minAppendSize, final long maxAppendSize
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
		this.minAppendSize = minAppendSize;
		this.maxAppendSize = maxAppendSize;
	}
	//
	@Override
	public final void submit(final WSObject wsObject) {
		if(wsObject!=null) {
			final long appendSize = ThreadLocalRandom
				.current()
				.nextLong(minAppendSize, maxAppendSize + 1);
			wsObject.append(appendSize);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Append the object \"{}\": +{}",
					Long.toHexString(wsObject.getId()), RunTimeConfig.formatSize(appendSize)
				);
			}
		}
		super.submit(wsObject);
	}
	//
}
