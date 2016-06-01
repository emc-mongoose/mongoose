package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
// mongoose-core-api.jar
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.HttpDataIoTask;
// mongoose-server-api.jar
import com.emc.mongoose.core.impl.io.task.BasicHttpDataIoTask;
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
		final String addrs[], final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			appConfig, reqConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			remoteLoadMap
		);
	}
	//
	protected BasicHttpDataLoadClient(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String addrs[], final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final Map<String, W> remoteLoadMap, final int instanceNum
	) throws RemoteException {
		super(
			appConfig, reqConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			remoteLoadMap, instanceNum
		);
	}
	//
	@Override
	protected HttpDataIoTask<T> getIoTask(final T item, final String nodeAddr) {
		return new BasicHttpDataIoTask<>(
			item, nodeAddr,  (HttpRequestConfig<T, ? extends Container<T>>) ioConfig
		);
	}
	//
}
