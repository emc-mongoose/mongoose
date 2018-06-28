package com.emc.mongoose.storage.driver.coop.net.http;

import com.emc.mongoose.env.DateUtil;
import com.emc.mongoose.exception.OmgDoesNotPerformException;
import com.emc.mongoose.supply.async.AsyncValueUpdatingSupplier;

import com.github.akurilov.fiber4j.FibersExecutor;

import java.util.Date;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateSupplier
extends AsyncValueUpdatingSupplier<String> {

	public AsyncCurrentDateSupplier(final FibersExecutor executor)
	throws OmgDoesNotPerformException {
		super(
			executor,
			DateUtil.FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis())),
			new CurrentDateInitCallable()
		);
	}

	private static final class CurrentDateInitCallable
	extends InitCallableBase<String> {
		@Override
		public final String call()
		throws Exception {
			return DateUtil.FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis()));
		}
	}
}
