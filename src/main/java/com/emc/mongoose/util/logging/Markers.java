package com.emc.mongoose.util.logging;
//
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
/**
 Created by kurila on 06.05.14.
 */
public final class Markers {
	//
	public final static Marker
		MSG = MarkerManager.getMarker("msg"),
		ERR = MarkerManager.getMarker("err"),
		DATA_LIST = MarkerManager.getMarker("dataList"),
		PERF_AVG = MarkerManager.getMarker("perfAvg"),
		PERF_SUM = MarkerManager.getMarker("perfSum"),
		PERF_TRACE = MarkerManager.getMarker("perfTrace");
	//
}
