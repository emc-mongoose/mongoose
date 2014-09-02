package com.emc.mongoose.object.http.impl;
//
import com.emc.mongoose.object.http.WSLoadExecutor;
import com.emc.mongoose.object.http.api.WSRequestConfig;
/**
 Created by kurila on 06.05.14.
 */
public class Delete
extends WSLoadExecutor {
	//
	public Delete(
		final String[] addrs, final WSRequestConfig sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
	}
	//
}
