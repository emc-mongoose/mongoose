package com.emc.mongoose.api.common.supply.async;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;

import java.util.Date;

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
			DateUtil.FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis())),
			new InitializedCallableBase<String>() {
				//
				@Override
				public final String call()
				throws Exception {
					return DateUtil.FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis()));
				}
			}
		);
	}
}
