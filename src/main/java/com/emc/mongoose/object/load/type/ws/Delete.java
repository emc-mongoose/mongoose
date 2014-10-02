package com.emc.mongoose.object.load.type.ws;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.WSLoadExecutorBase;
/**
 Created by kurila on 06.05.14.
 */
public class Delete<T extends WSObjectImpl>
extends WSLoadExecutorBase<T> {
	//
	public Delete(
		final String[] addrs, final WSRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
	}
	//
}
