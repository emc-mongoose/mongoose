package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
// mongoose-server-api.jar
import com.emc.mongoose.core.api.io.task.WSDataIOTask;
import com.emc.mongoose.core.impl.io.task.BasicWSDataIOTask;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.WSDataLoadClient;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
//
//import org.apache.log.log4j.Level;
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 08.05.14.
 */
public class BasicWSDataLoadClient<T extends HttpDataItem, W extends WSDataLoadSvc<T>>
extends LoadClientBase<T, W>
implements WSDataLoadClient<T, W> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSDataLoadClient(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String addrs[], final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			appConfig, reqConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			remoteLoadMap
		);
	}
	//
	@Override
	protected WSDataIOTask<T> getIOTask(final T item, final String nodeAddr) {
		return new BasicWSDataIOTask<>(
			item, nodeAddr,  (HttpRequestConfig<T, ? extends Container<T>>) ioConfigCopy
		);
	}
	//
}
