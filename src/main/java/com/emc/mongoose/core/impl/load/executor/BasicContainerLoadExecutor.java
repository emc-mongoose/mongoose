package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.impl.io.task.BasicContainerTask;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 20.10.15.
 */
public class BasicContainerLoadExecutor<T extends DataItem, C extends Container<T>>
extends LimitedRateLoadExecutorBase<C> {
	//
	public BasicContainerLoadExecutor(
		final RunTimeConfig rtConfig, final RequestConfig<C> reqConfig, final String[] addrs,
		final int connCountPerNode, final int threadCount, final ItemSrc<C> itemSrc,
		final long maxCount, final int manualTaskSleepMicroSecs, final float rateLimit
	) throws ClassCastException {
		super(
			rtConfig, reqConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			manualTaskSleepMicroSecs, rateLimit
		);
	}
	//
	@Override
	protected Future<? extends IOTask<C>> submitReqActually(final IOTask<C> request)
	throws RejectedExecutionException {
		return null;
	}
	//
	@Override
	protected IOTask<C> getIOTask(final C item, final String nextNodeAddr) {
		return new BasicContainerTask<>(item, nextNodeAddr, reqConfigCopy);
	}
	//
	@Override
	public int submitReqs(final List<? extends IOTask<C>> requests, final int from, final int to)
	throws RemoteException, RejectedExecutionException {
		return 0;
	}
}
