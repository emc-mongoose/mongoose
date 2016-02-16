package com.emc.mongoose.common.generator;

import java.util.Date;

public class AsyncDateGenerator extends AsyncRangeGeneratorBase<Date> {

	private AsyncLongGenerator longGenerator;

	public AsyncDateGenerator(Date minValue, Date maxValue) {
		super(minValue, maxValue);
		longGenerator =
				new AsyncLongGenerator(minValue.getTime(), maxValue.getTime());
	}

	public AsyncDateGenerator(Date initialValue) {
		super(initialValue);
		longGenerator = new AsyncLongGenerator(initialValue.getTime());
	}

	@Override
	protected Date computeRange(Date minValue, Date maxValue) {
		return null;
	}

	@Override
	protected Date rangeValue() {
		return new Date(longGenerator.get());
	}

	@Override
	protected Date singleValue() {
		Date date = null;
		try {
			date = new Date(longGenerator.get());
		} catch (Exception e) {
			singleValue();
		}
		return date;
	}
}
