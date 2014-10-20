package com.emc.mongoose.web.load.client;
//
import com.emc.mongoose.base.load.client.LoadClientImpl;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObjectImpl;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.util.Map;
import javax.management.remote.JMXConnector;
/**
 Created by kurila on 08.05.14.
 */
public class WSLoadClientImpl<T extends WSObjectImpl>
extends LoadClientImpl<T>
implements WSLoadClient<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public WSLoadClientImpl(
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
}
