package com.emc.mongoose.common.io;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.util.Date;

import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_RFC1123;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateInput
extends AsyncValueInput<String> {

	public static AsyncCurrentDateInput INSTANCE = null;

	static {
		try {
			INSTANCE = new AsyncCurrentDateInput();
		} catch(final OmgDoesNotPerformException e) {
			e.printStackTrace(System.err);
		}
	}

	private AsyncCurrentDateInput()
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
