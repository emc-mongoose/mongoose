package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.model.metrics.MetricsListener;

import java.io.Closeable;

/**
 Created by andrey on 05.07.17.
 */
public interface MeterMBean
extends Closeable, MetricsListener, MetricsContext.Snapshot {
	String METRICS_DOMAIN = MeterMBean.class.getPackage().getName();
}
