package com.emc.mongoose.ui.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created by kurila on 05.05.17.
 */
public interface Loggers {
	
	String BASE = Loggers.class.getPackage().getClass().getCanonicalName() + '.';
	String BASE_METRICS = BASE + "metrics.";
	String BASE_METRICS_THRESHOLD = BASE_METRICS + "threshold.";
	
	Logger CONFIG = LogManager.getLogger(BASE + "Config");
	Logger ERR = LogManager.getLogger(BASE + "Errors");
	Logger IO_TRACE = LogManager.getLogger(BASE + "IoTraces");
	Logger METRICS_EXT_RESULTS_FILE = LogManager.getLogger(BASE_METRICS + "ExtResultsFile");
	Logger METRICS_FILE = LogManager.getLogger(BASE_METRICS + "File");
	Logger METRICS_FILE_TOTAL = LogManager.getLogger(BASE_METRICS + "FileTotal");
	Logger METRICS_STD_OUT = LogManager.getLogger(BASE_METRICS + "StdOut");
	Logger METRICS_THRESHOLD_EXT_RESULTS_FILE = LogManager.getLogger(BASE_METRICS_THRESHOLD + "ExtResultsFile");
	Logger METRICS_THRESHOLD_FILE = LogManager.getLogger(BASE_METRICS_THRESHOLD + "File");
	Logger METRICS_THRESHOLD_FILE_TOTAL = LogManager.getLogger(BASE_METRICS_THRESHOLD + "FileTotal");
	Logger MSG = LogManager.getLogger("Messages");
	Logger MULTIPART = LogManager.getLogger("Multipart");
}
