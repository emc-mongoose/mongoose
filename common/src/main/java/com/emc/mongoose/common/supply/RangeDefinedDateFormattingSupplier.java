package com.emc.mongoose.common.supply;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 Created by kurila on 07.03.17.
 */
public class RangeDefinedDateFormattingSupplier
extends RangeDefinedLongSupplier
implements RangeDefinedSupplier<Date> {
	
	private DateFormat dtFormat;
	
	public RangeDefinedDateFormattingSupplier(final String formatStr) {
		this(new Date(0), new Date(), formatStr);
	}
	
	public RangeDefinedDateFormattingSupplier(
		final Date startDate, final Date endDate, final String formatStr
	) {
		super(startDate.getTime(), endDate.getTime());
		dtFormat = new SimpleDateFormat(formatStr);
	}
	
	private static ThreadLocal<Date> DATE = new ThreadLocal<Date>() {
		@Override
		protected final Date initialValue() {
			return new Date();
		}
	};
	
	@Override
	public final String get() {
		final Date date = DATE.get();
		date.setTime(getAsLong());
		return dtFormat.format(date);
	}
	
	@Override
	public final int get(final List<String> buffer, final int limit) {
		final long numbers[] = new long[limit];
		final int n = super.get(numbers, limit);
		final Date date = DATE.get();
		for(int i = 0; i < n; i ++) {
			date.setTime(numbers[i]);
			buffer.add(dtFormat.format(date));
		}
		return n;
	}
	
	@Override
	public final Date value() {
		return new Date(getAsLong());
	}
}
