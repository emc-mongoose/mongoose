package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.util.Date;

import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_RFC1123;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateSupplier
extends AsyncUpdatingValueSupplier<String> {

	public static AsyncCurrentDateSupplier INSTANCE = null;

	static {
		try {
			INSTANCE = new AsyncCurrentDateSupplier();
		} catch(final OmgDoesNotPerformException e) {
			e.printStackTrace(System.err);
		}
	}

	private AsyncCurrentDateSupplier()
	throws OmgDoesNotPerformException {
		super(
			FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis())),
			new InitializedCallableBase<String>() {
				//
				@Override
				public final String call()
				throws Exception {
					return FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis()));
				}
			}
		);
	}
}
