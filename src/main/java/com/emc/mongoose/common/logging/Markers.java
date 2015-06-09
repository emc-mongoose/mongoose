package com.emc.mongoose.common.logging;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Created by olga on 09.06.15.
 */
public interface Markers {
	//
	Marker
		MSG = MarkerManager.getMarker("msg"),
		ERR = MarkerManager.getMarker("err"),
		DATA_LIST = MarkerManager.getMarker("dataList"),
		PERF_AVG = MarkerManager.getMarker("perfAvg"),
		PERF_SUM = MarkerManager.getMarker("perfSum"),
		PERF_TRACE = MarkerManager.getMarker("perfTrace");
}
