package com.emc.mongoose.object.load.type.ws;
//
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.object.data.WSDataObjectImpl;
import com.emc.mongoose.object.load.WSLoadExecutorBase;
/**
 Created by kurila on 06.05.14.
 */
public class Delete<T extends WSDataObjectImpl>
extends WSLoadExecutorBase<T> {
	//
	public Delete(
		final String[] addrs, final WSObjectRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
	}
	//
}
