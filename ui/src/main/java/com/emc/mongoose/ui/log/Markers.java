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
	Marker METRICS_STDOUT = MarkerManager.getMarker("metricsStdout");
	Marker METRICS_MED_STDOUT = MarkerManager.getMarker("metricsMedStdout");
	Marker METRICS_FILE = MarkerManager.getMarker("metricsFile");
	Marker METRICS_MED_FILE = MarkerManager.getMarker("metricsMedFile");
	Marker METRICS_FILE_TOTAL = MarkerManager.getMarker("metricsFileTotal");
	Marker METRICS_MED_FILE_TOTAL = MarkerManager.getMarker("metricsMedFileTotal");
	Marker METRICS_EXT_RESULTS = MarkerManager.getMarker("extResultsFile");
	Marker IO_TRACE = MarkerManager.getMarker("ioTrace");
	Marker CFG = MarkerManager.getMarker("cfg");
	Marker MPU = MarkerManager.getMarker("mpu");
}
