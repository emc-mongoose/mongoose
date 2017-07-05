package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.model.metrics.MetricsListener;

/**
 Created by andrey on 05.07.17.
 */
public interface MeterMBean
extends MetricsListener, MetricsContext.Snapshot {
}
