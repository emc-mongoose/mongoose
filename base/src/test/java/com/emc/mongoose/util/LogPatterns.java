package com.emc.mongoose.util;

import java.util.regex.Pattern;

/** Created by kurila on 26.01.17. */
public interface LogPatterns {

	String KEY_STEP_ID = "Load Step Id";
	String KEY_OP_TYPE = "Operation Type";
	String KEY_NODE_COUNT = "Node Count";
	String KEY_CONCURRENCY = "Concurrency";
	String KEY_CONCURRENCY_LIMIT = "Limit Per Storage Driver";
	String KEY_CONCURRENCY_ACTUAL = "Actual";
	String KEY_LAST = "Last";
	String KEY_MEAN = "Mean";
	String KEY_OP_COUNT = "Operations Count";
	String KEY_SUCC = "Successful";
	String KEY_FAIL = "Failed";
	String KEY_SIZE = "Transfer Size";
	String KEY_DURATION = "Duration [s]";
	String KEY_ELAPSED = "Elapsed";
	String KEY_SUM = "Sum";
	String KEY_TP = "Throughput [op/s]";
	String KEY_BW = "Bandwidth [MB/s]";
	String KEY_OP_DUR = "Operations Duration [us]";
	String KEY_OP_LAT = "Operations Latency [us]";
	String KEY_AVG = "Avg";
	String KEY_MIN = "Min";
	String KEY_LOQ = "LoQ";
	String KEY_MED = "Med";
	String KEY_HIQ = "HiQ";
	String KEY_MAX = "Max";

	// common
	Pattern ASCII_COLOR = Pattern.compile("\\u001B\\[?m?[\\u001B\\[0-9m;]*");

	Pattern DATE_TIME_ISO8601 = Pattern.compile(
					"(?<dateTime>[\\d]{4}-[\\d]{2}-[\\d]{2}T(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2},[\\d]{3}))");
	Pattern STD_OUT_LOG_LEVEL = Pattern.compile("(?<levelLog>[FEWIDT])");
	Pattern STD_OUT_CLASS_NAME = Pattern.compile("[A-Za-z]+[\\w]*");
	Pattern STD_OUT_THREAD_NAME = Pattern.compile("(?<nameThread>\\w[\\w\\s#.\\-<>]+\\w)");

	// metrics
	Pattern OP_TYPE = Pattern.compile(
					ASCII_COLOR.pattern() + "(?<opType>[CREATDLUPNO]{4,6})\\s{0,2}" + ASCII_COLOR.pattern());
	Pattern STD_OUT_METRICS_TABLE_ROW = Pattern.compile(
					"\\s*(?<stepName>[\\w\\-_.,;:~=+@]{1,10})\\|(?<timestamp>[\\d]{12})"
									+ "\\|"
									+ OP_TYPE.pattern()
									+ "\\|\\s*(?<concurrencyCurr>[\\d]{1,10})"
									+ "\\|(?<concurrencyLastMean>[\\d]+\\.?[\\d]*)\\s*"
									+ "\\|\\s*(?<succCount>[\\d]{1,12})"
									+ "\\|\\s*"
									+ ASCII_COLOR.pattern()
									+ "\\s*(?<failCount>[\\d]{1,6})"
									+ ASCII_COLOR.pattern()
									+ "\\|(?<stepTime>[\\d]+\\.?[\\d]*)\\s*"
									+ "\\|(?<tp>[\\d]+\\.?[\\d]*)\\.?\\s*\\|(?<bw>[\\d]+\\.?[\\d]*)\\.?\\s*"
									+ "\\|\\s*(?<lat>[\\d]{1,10})"
									+ "\\|\\s*(?<dur>[\\d]{1,11})");
	Pattern STD_OUT_METRICS_SUMMARY = Pattern.compile(
					"\\s*[\\-]{3}\\s#\\sResults\\s[#]{110}\n(?<content>[\\w\\s\\.:\\-\\[\\]/\n]+)[\\.]{3}",
					Pattern.MULTILINE);

	Pattern STD_OUT_LOAD_THRESHOLD_ENTRANCE = Pattern.compile(
					ASCII_COLOR.pattern()
									+ DATE_TIME_ISO8601.pattern()
									+ "\\s+"
									+ STD_OUT_LOG_LEVEL.pattern()
									+ "\\s+"
									+ STD_OUT_CLASS_NAME.pattern()
									+ "\\s+"
									+ STD_OUT_THREAD_NAME.pattern()
									+ "\\s+[\\-_#@\\(\\)\\w]+:\\s+the threshold of (?<threshold>[0-9]+) active tasks count is reached, "
									+ "starting the additional metrics accounting");

	Pattern STD_OUT_LOAD_THRESHOLD_EXIT = Pattern.compile(
					ASCII_COLOR.pattern()
									+ DATE_TIME_ISO8601.pattern()
									+ "\\s+"
									+ STD_OUT_LOG_LEVEL.pattern()
									+ "\\s+"
									+ STD_OUT_CLASS_NAME.pattern()
									+ "\\s+"
									+ STD_OUT_THREAD_NAME.pattern()
									+ "\\s+[\\-_#@\\(\\)\\w]+:\\s+the active tasks count is below the threshold of (?<threshold>[0-9]+), "
									+ "stopping the additional metrics accounting");
}
