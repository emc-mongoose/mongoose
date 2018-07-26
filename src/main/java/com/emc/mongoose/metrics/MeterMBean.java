package com.emc.mongoose.metrics;

/**
 Created by andrey on 05.07.17.
 */
public interface MeterMBean
extends AutoCloseable, MetricsListener, MetricsSnapshot {

	String METRICS_DOMAIN = MeterMBean.class.getPackage().getName();

	String KEY_OP_TYPE = "op_type";
	String KEY_NODE_COUNT = "node_count";
	String KEY_CONCURRENCY_LIMIT = "concurrency_limit";
	String KEY_ITEM_DATA_SIZE = "item_data_size";
}
