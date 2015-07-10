package com.emc.mongoose.integ.integTestTools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Created by olga on 03.07.15.
 */
public interface IntegConstants {
	//
	String SCENARIO_END_INDICATOR = "Scenario end";
	String SUMMARY_INDICATOR = "summary:";
	String CONTENT_MISMATCH_INDICATOR = ": content mismatch @ offset 0, expected:";
	String PORT_INDICATOR = ":902";
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
	String KEY_VERIFY_CONTENT = "load.type.read.verifyContent";
	//
	String LOAD_CREATE = "Create";
	String LOAD_READ = "Read";
	String LOAD_UPDATE = "Update";
	String LOAD_APPEND = "Append";
	String LOAD_DELETE = "Delete";
	//
	String SCENARIO_SINGLE = "single";
	String SCENARIO_CHAIN = "chain";
	String SCENARIO_RAMPUP = "rampup";
	//
	String BYTE = "B";
	String MBYTE = "MB";
	String KBYTE = "KB";
	String GBYTE = "GB";
	String TBYTE = "TB";
	String EBYTE = "EB";
	//
	String API_S3 = "s3";
	//
	int COUNT_SUCC_COLUMN_INDEX = 8;
	int COUNT_FAIL_COLUMN_INDEX = 9;
	int DATA_SIZE_COLUMN_INDEX = 2;
	int DATA_ID_COLUMN_INDEX = 0;
	int DATA_ITEMS_COLUMN_COUNT = 4;
	//
	DateFormat FMT_DT = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", Locale.ROOT) {
		{ setTimeZone(TimeZone.getTimeZone("UTC")); }
	};
	//
	Pattern LOAD_THRED_NAME_PATTERN = Pattern.compile(
		"[0-9]+-(S3|ATMOS|SWIFT)-(Create|Read|Update|Append|Delete)([0-9]+)?-([0-9]+x)?[0-9]+x[0-9]+#[0-9]+"
	);
	Pattern LOAD_PATTERN = Pattern.compile(
		"[0-9]+-(S3|ATMOS|SWIFT)-(Create|Read|Update|Append|Delete)([0-9]+)?-([0-9]+x)?[0-9]+x[0-9]+"
	);
	Pattern TIME_PATTERN = Pattern.compile("[0-9]{2}:[0-9]{2}:[0-9]{2}");
	Pattern LOAD_NAME_PATTERN = Pattern.compile("(Create|Read|Update|Append|Delete)");

}
