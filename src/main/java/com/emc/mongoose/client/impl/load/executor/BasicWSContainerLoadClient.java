package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.WSContainerLoadClient;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.WSContainerIOTask;
//
import com.emc.mongoose.core.impl.io.task.BasicWSContainerTask;
//
import com.emc.mongoose.server.api.load.executor.WSContainerLoadSvc;
//
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 21.10.15.
 */
public class BasicWSContainerLoadClient<
	T extends WSObject, C extends Container<T>, W extends WSContainerLoadSvc<T, C>
> extends LoadClientBase<C, W> implements WSContainerLoadClient<T, C, W> {
	//
	public BasicWSContainerLoadClient(
		final RunTimeConfig rtConfig, final WSRequestConfig reqConfig, final String addrs[],
		final int connCountPerNode, final int threadCount,
		final ItemSrc<C> itemSrc, final long maxCount,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			rtConfig, reqConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			remoteLoadMap
		);
	}
	//
	@Override
	protected WSContainerIOTask<T, C> getIOTask(final C item, final String nodeAddr) {
		return new BasicWSContainerTask<>(item, nodeAddr, (WSRequestConfig) ioConfigCopy);
	}
	//
}
