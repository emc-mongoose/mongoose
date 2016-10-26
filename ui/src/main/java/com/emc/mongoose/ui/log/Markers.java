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
	Marker METRICS_PERIODIC_STDOUT = MarkerManager.getMarker("metricsPeriodicStdout");
	Marker METRICS_TOTAL_STDOUT = MarkerManager.getMarker("metricsTotalStdout");
	Marker METRICS_MED_STDOUT = MarkerManager.getMarker("metricsMedStdout");
	Marker METRICS_PERIODIC_FILE = MarkerManager.getMarker("metricsPeriodicFile");
	Marker METRICS_TOTAL_FILE = MarkerManager.getMarker("metricsTotalFile");
	Marker METRICS_MED_FILE = MarkerManager.getMarker("metricsMedFile");
	Marker IO_TRACE = MarkerManager.getMarker("ioTrace");
	Marker CFG = MarkerManager.getMarker("cfg");
}
