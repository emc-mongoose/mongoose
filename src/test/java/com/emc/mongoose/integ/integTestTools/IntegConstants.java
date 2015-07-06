package com.emc.mongoose.integ.integTestTools;

/**
 * Created by olga on 03.07.15.
 */
public interface IntegConstants {
	//
	String SCENARIO_END_INDICATOR = "Scenario end";
	String SUMMARY_INDICATOR = "summary:";
	//
	String	MESSAGE_FILE_NAME = "messages.log";
	String PERF_AVG_FILE_NAME = "perf.avg.csv";
	String PERF_SUM_FILE_NAME = "perf.sum.csv";
	String PERF_TRACE_FILE_NAME = "perf.trace.csv";
	String DATA_ITEMS_FILE_NAME = "data.items.csv";
	String ERR_FILE_NAME = "errors.log";
	//
	String LOG_CONF_PROPERTY_KEY = "log4j.configurationFile";
	String LOG_FILE_NAME = "logging.json";
	String USER_DIR_PROPERTY_NAME = "user.dir";
	//
	String LOAD_CREATE = "create";
	String LOAD_READ = "read";
	String LOAD_UPDATE = "update";
	String LOAD_APPEND = "append";
	String LOAD_DELETE = "delete";
	//
	String BYTE = "B";
	String MByte = "MB";
	String KByte = "KB";
	String GByte = "GB";
	String TByte = "TB";
	String EByte = "EB";
	//
	int COUNT_SUCC_COLUMN_INDEX = 8;
	int DATA_SIZE_COLUMN_INDEX = 2;
	int DATA_ID_COLUMN_INDEX = 0;
	int DATA_ITEMS_COLUMN_COUNT = 4;
	//

}
