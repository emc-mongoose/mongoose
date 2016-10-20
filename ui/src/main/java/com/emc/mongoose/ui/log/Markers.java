package com.emc.mongoose.ui.log;
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
	Marker METRICS_PERIODIC = MarkerManager.getMarker("metricsPeriodic");
	Marker METRICS_TOTAL = MarkerManager.getMarker("metricsTotal");
	Marker METRICS_MED = MarkerManager.getMarker("metricsMed");
	Marker IO_TRACE = MarkerManager.getMarker("ioTrace");
	Marker CFG = MarkerManager.getMarker("cfg");
}
