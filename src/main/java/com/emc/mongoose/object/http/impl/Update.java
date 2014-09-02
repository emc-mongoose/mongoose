package com.emc.mongoose.object.http.impl;
//
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.WSLoadExecutor;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.api.WSRequestConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
/**
 Created by kurila on 06.05.14.
 */
public class Update
extends WSLoadExecutor {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int updatesPerObject;
	//
	public Update(
		final String[] addrs, final WSRequestConfig sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile, final int updatesPerObject
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
		this.updatesPerObject = updatesPerObject;
	}
	//
	@Override
	public final void submit(final WSObject wsObject) {
		if(wsObject!=null) {
			try {
				wsObject.getRanges().createRandom(updatesPerObject);
			} catch(final IOException e) {
				LOG.warn(Markers.ERR, "Failed to create modified ranges: ", e.toString());
				if(LOG.isTraceEnabled()) {
					final Throwable cause = e.getCause();
					if(cause!=null) {
						LOG.trace(Markers.ERR, cause.toString(), cause.getCause());
					}
				}
			}
			if(LOG.isTraceEnabled()) {
				LOG.trace(
					Markers.MSG, "Modified {}/{} ranges for object \"{}\"",
					wsObject.getRanges().getCount(), updatesPerObject,
					Long.toHexString(wsObject.getId())
				);
			}
		}
		super.submit(wsObject);
	}
}
