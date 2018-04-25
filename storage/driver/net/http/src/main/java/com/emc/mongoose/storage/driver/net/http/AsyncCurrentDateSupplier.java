package com.emc.mongoose.storage.driver.net.http;

import com.emc.mongoose.api.common.env.DateUtil;
import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.api.common.supply.async.AsyncUpdatingValueSupplier;

import com.github.akurilov.concurrent.coroutine.CoroutinesExecutor;

import java.util.Date;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateSupplier
extends AsyncUpdatingValueSupplier<String> {

	public AsyncCurrentDateSupplier(final CoroutinesExecutor executor)
	throws OmgDoesNotPerformException {
		super(
			executor,
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
