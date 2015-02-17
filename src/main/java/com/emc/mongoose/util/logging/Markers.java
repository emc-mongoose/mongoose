package com.emc.mongoose.util.logging;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LifeCycle;
/**
 Created by kurila on 06.05.14.
 */
public final class Markers {
	//
	static {
		Runtime.getRuntime().addShutdownHook(
			new Thread("loggingShutDownHook") {
				@Override
				public final void run() {
					((LifeCycle) LogManager.getContext()).stop();
				}
			}
		);
	}
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
