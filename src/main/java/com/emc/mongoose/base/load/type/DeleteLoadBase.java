package com.emc.mongoose.base.load.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.impl.LoadExecutorBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
/**
 Created by kurila on 20.10.14.
 */
public abstract class DeleteLoadBase<T extends DataItem>
extends LoadExecutorBase<T> {
	protected DeleteLoadBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final RequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(runTimeConfig, addrs, sharedReqConf, maxCount, threadsPerNode, listFile);
	}
}
