package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.server.api.load.executor.LoadSvc;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
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
		final RunTimeConfig runTimeConfig, final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap, final WSRequestConfig<T> reqConf,
		final long maxCount
	) {
		super(
			runTimeConfig, remoteLoadMap, remoteJMXConnMap, reqConf, maxCount
		);
	}
}
