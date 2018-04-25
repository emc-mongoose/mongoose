package com.emc.mongoose.api.common.supply.async;

import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.common.supply.PatternDefinedSupplier;
import com.emc.mongoose.api.common.supply.RangePatternDefinedSupplier;

import com.github.akurilov.concurrent.coroutine.CoroutinesExecutor;

public final class AsyncPatternDefinedSupplier
extends AsyncUpdatingValueSupplier<String>
implements PatternDefinedSupplier {
	
	private final PatternDefinedSupplier wrappedSupplier;
	
	public AsyncPatternDefinedSupplier(final CoroutinesExecutor executor, final String pattern)
	throws OmgShootMyFootException {
		this(
			executor,
			new RangePatternDefinedSupplier(
				pattern, AsyncStringSupplierFactory.getInstance(executor)
			)
		);
	}
	
	private AsyncPatternDefinedSupplier(
		final CoroutinesExecutor executor, final PatternDefinedSupplier wrappedSupplier
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
