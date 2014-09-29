package com.emc.mongoose.object.load.impl.type;
//
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.object.load.impl.WSLoadExecutor;
import com.emc.mongoose.object.data.WSDataObject;
/**
 Created by kurila on 06.05.14.
 */
public class WSDelete<T extends WSDataObject>
extends WSLoadExecutor<T> {
	//
	public WSDelete(
		final String[] addrs, final WSObjectRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
	}
	//
}
