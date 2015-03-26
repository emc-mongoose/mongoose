package com.emc.mongoose.client.api.load.executor;
// mongoose-common.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import java.util.Map;
/**
 Created by andrey on 30.09.14.
 A client-side handler for controlling remote (server-size) load execution.
 */
public interface LoadClient<T extends DataItem>
extends LoadExecutor<T> {
	String
		DEFAULT_DOMAIN = "metrics",
		FMT_MSG_FAIL_FETCH_VALUE = "Failed to fetch the value for \"%s\" from %s",
		KEY_NAME = "name",
		ATTR_COUNT = "Count",
		ATTR_MIN = "Min",
		ATTR_AVG = "Mean",
		ATTR_MAX = "Max",
		ATTR_MED = "50thPercentile",
		ATTR_75P = "75thPercentile",
		ATTR_95P = "95thPercentile",
		ATTR_98P = "98thPercentile",
		ATTR_99P = "99thPercentile",
		ATTR_RATE_MEAN = "MeanRate",
		ATTR_RATE_1MIN = "OneMinuteRate",
		ATTR_RATE_5MIN = "FiveMinuteRate",
		ATTR_RATE_15MIN = "FifteenMinuteRate";
	Map<String, LoadSvc<T>> getRemoteLoadMap();
	void logMetaInfoFrames();
}
