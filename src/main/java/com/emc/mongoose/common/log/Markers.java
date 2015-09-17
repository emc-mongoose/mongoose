package com.emc.mongoose.common.log;
//
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
/**
 Created by kurila on 09.06.15.
 */
public interface Markers {
	//
	Marker MSG = MarkerManager.getMarker("msg");
	Marker ERR = MarkerManager.getMarker("err");
	Marker DATA_LIST = MarkerManager.getMarker("dataList");
	Marker PERF_AVG = MarkerManager.getMarker("perfAvg");
	Marker PERF_SUM = MarkerManager.getMarker("perfSum");
	Marker PERF_TRACE = MarkerManager.getMarker("perfTrace");
	Marker CFG = MarkerManager.getMarker("cfg");
}
