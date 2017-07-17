package com.emc.mongoose.api.metrics;

import com.emc.mongoose.api.model.metrics.MetricsContext;
import com.emc.mongoose.api.model.metrics.MetricsListener;

import java.io.Closeable;

/**
 Created by andrey on 05.07.17.
 */
public interface MeterMBean
extends Closeable, MetricsListener, MetricsContext.Snapshot {
	String METRICS_DOMAIN = MetricsContext.class.getPackage().getName();
	String KEY_LOAD_TYPE = "loadType";
	String KEY_STORAGE_DRIVER_COUNT = "storageDriverCount";
	String KEY_STORAGE_DRIVER_CONCURRENCY = "storageDriverConcurrency";
}
