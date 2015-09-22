package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
//
//import org.apache.log.log4j.Level;
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
//
import java.util.Map;
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
		final WSRequestConfig<T> reqConf, final long maxCount, final DataItemSrc<T> itemSrc
	) {
		super(runTimeConfig, remoteLoadMap, reqConf, maxCount, itemSrc);
	}
}
