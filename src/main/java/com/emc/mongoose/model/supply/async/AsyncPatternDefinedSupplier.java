package com.emc.mongoose.model.supply.async;

import com.emc.mongoose.model.exception.OmgDoesNotPerformException;
import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.supply.PatternDefinedSupplier;
import com.emc.mongoose.model.supply.RangePatternDefinedSupplier;

import com.github.akurilov.fiber4j.FibersExecutor;

public final class AsyncPatternDefinedSupplier
extends AsyncUpdatingValueSupplier<String>
implements PatternDefinedSupplier {
	
	private final PatternDefinedSupplier wrappedSupplier;
	
	public AsyncPatternDefinedSupplier(final FibersExecutor executor, final String pattern)
	throws OmgShootMyFootException {
		this(
			executor,
			new RangePatternDefinedSupplier(
				pattern, AsyncStringSupplierFactory.getInstance(executor)
			)
		);
	}
	
	private AsyncPatternDefinedSupplier(
		final FibersExecutor executor, final PatternDefinedSupplier wrappedSupplier
	) throws OmgDoesNotPerformException {
		super(
			executor,
			null,
			new InitializedCallableBase<String>() {
				private final StringBuilder result = new StringBuilder();
				@Override
				public final String call()
				throws Exception {
					result.setLength(0);
					return wrappedSupplier.format(result);
				}
			}
		);
		this.wrappedSupplier = wrappedSupplier;
	}
	
	@Override
	public String getPattern() {
		return wrappedSupplier.getPattern();
	}

	@Override
	public String format(final StringBuilder result) {
		return wrappedSupplier.format(result);
	}
}
