package com.emc.mongoose.api.model.concurrent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.emc.mongoose.api.common.concurrent.StoppableTask;

/**
 * Created by kurila on 26.07.17.
 */
public interface Coroutine
extends StoppableTask {
}
