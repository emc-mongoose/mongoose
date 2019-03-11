package com.emc.mongoose.base.metrics;

import com.emc.mongoose.base.Constants;

public interface MetricsConstants {

	String METRIC_NAME_DUR = "duration";
	String METRIC_NAME_LAT = "latency";
	String METRIC_NAME_CONC = "concurrency";
	String METRIC_NAME_SUCC = "success_op";
	String METRIC_NAME_FAIL = "failed_op";
	String METRIC_NAME_BYTE = "byte";
	String METRIC_NAME_TIME = "elapsed_time";
	String[] METRIC_LABELS = {
			"load_step_id",
			"load_op_type",
			"storage_driver_limit_concurrency",
			"item_data_size",
			"start_time",
			"node_list",
			"user_comment"
	};
	String METRIC_FORMAT = Constants.APP_NAME + "_%s"; // appName_metricName<_aggregationType>
}
