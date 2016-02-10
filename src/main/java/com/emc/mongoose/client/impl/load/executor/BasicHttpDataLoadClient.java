package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.HttpDataIOTask;
// mongoose-server-api.jar
import com.emc.mongoose.core.impl.io.task.BasicHttpDataIOTask;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.HttpDataLoadClient;
//
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
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
public class BasicHttpDataLoadClient<T extends HttpDataItem, W extends HttpDataLoadSvc<T>>
extends LoadClientBase<T, W>
implements HttpDataLoadClient<T, W> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpDataLoadClient(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String addrs[], final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(appConfig, reqConfig, addrs, threadCount, itemSrc, maxCount, remoteLoadMap);
	}
	//
	@Override
	protected HttpDataIOTask<T> getIOTask(final T item, final String nodeAddr) {
		return new BasicHttpDataIOTask<>(
			item, nodeAddr,  (HttpRequestConfig<T, ? extends Container<T>>) ioConfigCopy
		);
	}
	//
}
