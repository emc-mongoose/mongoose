package com.emc.mongoose.metrics;

public interface MetricsConstants {

	String METRIC_NAME_DUR = "duration";
	String METRIC_NAME_LAT = "latency";
	String METRIC_NAME_CONC = "concurrency";
	String METRIC_NAME_SUCC = "operations_successful";
	String METRIC_NAME_FAIL = "operations_failed";
	String METRIC_NAME_BYTE = "transferred_bytes";
	String METRIC_NAME_TIME = "elapsed_time";

	String[] METRIC_LABELS = {
		"load_step_id",
		"load_op_type",
		"storage_driver_limit_concurrency",
		"node_count",
		"item_data_size",
	};
}
