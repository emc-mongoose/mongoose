package com.emc.mongoose.common.supply;

import org.apache.commons.lang.time.FastDateFormat;

import java.text.Format;
import java.util.Date;
import java.util.List;

/**
 Created by kurila on 07.03.17.
 */
public class RangeDefinedDateFormattingSupplier
extends RangeDefinedLongSupplier
implements RangeDefinedSupplier<Date> {
	
	private Format format;
	
	public RangeDefinedDateFormattingSupplier() {
		this(new Date(0), new Date(), null);
	}
	
	public RangeDefinedDateFormattingSupplier(final Date startDate, final Date endDate) {
		this(startDate, endDate, null);
	}
	
	public RangeDefinedDateFormattingSupplier(final String formatStr) {
		this(new Date(0), new Date(), formatStr);
	}
	
	public RangeDefinedDateFormattingSupplier(
		final Date startDate, final Date endDate, final String formatStr
	) {
		super(startDate.getTime(), endDate.getTime());
		format = formatStr == null || formatStr.isEmpty() ?
			null : FastDateFormat.getInstance(formatStr);
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
		return format == null ? date.toString() : format.format(date);
	}
	
	@Override
	public final int get(final List<String> buffer, final int limit) {
		final long numbers[] = new long[limit];
		final int n = super.get(numbers, limit);
		final Date date = DATE.get();
		if(format == null) {
			for(int i = 0; i < n; i ++) {
				date.setTime(numbers[i]);
				buffer.add(date.toString());
			}
		} else {
			for(int i = 0; i < n; i ++) {
				date.setTime(numbers[i]);
				buffer.add(format.format(date));
			}
		}
		return n;
	}
	
	@Override
	public final Date value() {
		return new Date(getAsLong());
	}
}
