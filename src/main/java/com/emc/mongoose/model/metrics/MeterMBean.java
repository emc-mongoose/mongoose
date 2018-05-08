package com.emc.mongoose.model.metrics;

/**
 Created by andrey on 05.07.17.
 */
public interface MeterMBean
extends AutoCloseable, MetricsListener, MetricsSnapshot {
	String METRICS_DOMAIN = MeterMBean.class.getPackage().getName();
	String KEY_LOAD_TYPE = "loadType";
	String KEY_STORAGE_DRIVER_COUNT = "storageDriverCount";
	String KEY_STORAGE_DRIVER_CONCURRENCY = "storageDriverConcurrency";
}
