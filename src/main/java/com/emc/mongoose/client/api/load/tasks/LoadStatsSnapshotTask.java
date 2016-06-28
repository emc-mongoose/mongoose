package com.emc.mongoose.client.api.load.tasks;

import com.emc.mongoose.core.api.load.model.metrics.IoStats;

/**
 Created by kurila on 28.06.16.
 */
public interface LoadStatsSnapshotTask
extends Runnable {

	int COUNT_LIMIT_RETRIES = 100;

	IoStats.Snapshot getLastStatsSnapshot();
}
