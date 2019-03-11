package com.emc.mongoose.base.metrics.context;

import com.emc.mongoose.base.item.op.OpType;
import com.github.akurilov.commons.system.SizeInBytes;
import java.util.function.IntSupplier;

/** @author veronika K. on 21.11.18 */
public interface ContextBuilder<B extends ContextBuilder, C extends MetricsContext> {

	C build();

	B id(final String id);

	B comment(final String comment);

	B opType(final OpType opType);

	B concurrencyLimit(final int concurrencyLimit);

	B concurrencyThreshold(final int concurrencyThreshold);

	B itemDataSize(final SizeInBytes itemDataSize);

	B stdOutColorFlag(final boolean stdOutColorFlag);

	B outputPeriodSec(final int outputPeriodSec);

	B actualConcurrencyGauge(final IntSupplier actualConcurrencyGauge);
}
