package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshot;

/** Created by andrey on 06.07.17. The entity accepting the metrics updates */
@FunctionalInterface
public interface DistributedMetricsListener {

  /**
   * Update the state with the snapshot
   *
   * @param snapshot the metrics snapshot to update the metrics listener state
   */
  void notify(final DistributedAllMetricsSnapshot snapshot);
}
