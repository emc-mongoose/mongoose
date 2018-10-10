package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.github.akurilov.commons.system.SizeInBytes;

/**
 @author veronika K. on 10.10.18 */
public abstract class DistributedMetricsContextBase<S extends DistributedMetricsSnapshot>
extends MetricsContextBase<S>
implements DistributedMetricsContext<S>
{

	protected DistributedMetricsContextBase(
		final String id, final OpType opType, final int concurrencyLimit, final int nodeCount,
		final int concurrencyThreshold,
		final SizeInBytes itemDataSize,
		final boolean stdOutColorFlag,
		final long outputPeriodMillis
	) {
		super(id, opType, concurrencyLimit, nodeCount, concurrencyThreshold, itemDataSize, stdOutColorFlag,
			outputPeriodMillis
		);
	}
}
