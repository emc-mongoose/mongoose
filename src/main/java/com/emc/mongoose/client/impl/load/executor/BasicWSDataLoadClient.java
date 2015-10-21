package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
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
public class BasicWSDataLoadClient<T extends WSObject, W extends WSDataLoadSvc<T>>
extends LoadClientBase<T, W>
implements WSDataLoadClient<T, W> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSDataLoadClient(
		final RunTimeConfig rtConfig, final WSRequestConfig<T> reqConfig, final String addrs[],
		final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			rtConfig, reqConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			remoteLoadMap
		);
	}
	//
	@Override
	protected WSDataIOTask<T> getIOTask(final T item, final String nodeAddr) {
		return new BasicWSDataIOTask<>(item, nodeAddr, (WSRequestConfig<T>) reqConfigCopy);
	}
	//
}
