package com.emc.mongoose.api.common.supply.async;

import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.common.supply.PatternDefinedSupplier;
import com.emc.mongoose.api.common.supply.RangePatternDefinedSupplier;

public final class AsyncPatternDefinedSupplier
extends AsyncUpdatingValueSupplier<String>
implements PatternDefinedSupplier {
	
	private final PatternDefinedSupplier wrappedSupplier;
	
	public AsyncPatternDefinedSupplier(final String pattern)
	throws UserShootHisFootException {
		this(new RangePatternDefinedSupplier(pattern, AsyncStringSupplierFactory.getInstance()));
	}
	
	private AsyncPatternDefinedSupplier(final PatternDefinedSupplier wrappedSupplier)
	throws OmgDoesNotPerformException {
		super(
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
