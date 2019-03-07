package com.emc.mongoose.base.metrics.context;

import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** @author veronika K. on 21.11.18 */
public interface DistributedContextBuilder
				extends ContextBuilder<DistributedContextBuilder, DistributedMetricsContextImpl> {

	DistributedContextBuilder quantileValues(final List<Double> quantileValues);

	DistributedContextBuilder nodeAddrs(final List<String> nodeAddrs);

	DistributedContextBuilder nodeCountSupplier(final IntSupplier nodeCountSupplier);

	DistributedContextBuilder snapshotsSupplier(
					final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier);

	DistributedContextBuilder avgPersistFlag(final boolean avgPersistFlag);

	DistributedContextBuilder sumPersistFlag(final boolean sumPersistFlag);
}
