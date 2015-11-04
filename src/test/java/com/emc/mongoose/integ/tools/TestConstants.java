package com.emc.mongoose.integ.tools;

/**
 * Created by olga on 03.07.15.
 */
public interface TestConstants {
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
	String ITEMS_FILE_NAME = "items.csv";
	String ERR_FILE_NAME = "errors.log";
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
	String API_S3 = "s3";
	//
	String BUCKET_NAME = "bucketTest";
}
