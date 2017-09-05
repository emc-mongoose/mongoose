package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.api.common.supply.async.AsyncUpdatingValueSupplier;
import com.github.akurilov.coroutines.CoroutinesProcessor;

import java.util.Date;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateSupplier
extends AsyncUpdatingValueSupplier<String> {

	public AsyncCurrentDateSupplier(final CoroutinesProcessor coroutinesProcessor)
	throws OmgDoesNotPerformException {
		super(
			coroutinesProcessor,
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
