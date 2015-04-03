package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
//
//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
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
		final long maxCount, final Producer<T> producer
	) {
		super(
			runTimeConfig, remoteLoadMap, remoteJMXConnMap, reqConf, maxCount, producer
		);
	}
}
