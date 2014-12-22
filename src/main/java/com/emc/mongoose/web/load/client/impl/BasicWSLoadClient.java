package com.emc.mongoose.web.load.client.impl;
//
import com.emc.mongoose.base.load.client.impl.BasicLoadClient;
import com.emc.mongoose.base.load.server.LoadSvc;
//import com.emc.mongoose.util.logging.ExceptionHandler;
//import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.web.load.client.WSLoadClient;
import com.emc.mongoose.web.load.server.WSLoadSvc;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.Map;
import javax.management.remote.JMXConnector;
/**
 Created by kurila on 08.05.14.
 */
public class BasicWSLoadClient<T extends WSObject>
extends BasicLoadClient<T>
implements WSLoadClient<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadClient(
		final RunTimeConfig runTimeConfig,
		final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap,
		final WSRequestConfig<T> reqConf,
		final long maxCount, final int threadCountPerServer
	) {
		super(
			runTimeConfig, remoteLoadMap, remoteJMXConnMap, reqConf, maxCount, threadCountPerServer
		);
	}
	//
	@Override
	public final HttpResponse execute(final HttpHost tgtHost, final HttpRequest request)
	throws RemoteException {
		final Object addrs[] = remoteLoadMap.keySet().toArray();
		final String addr = String.class.cast(
			addrs[(int) getTaskCount() % addrs.length]
		);
		return ((WSLoadSvc<T>) remoteLoadMap.get(addr)).execute(tgtHost, request);
	}
}
