package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.log.LogUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AsyncDateGenerator
extends AsyncRangeGeneratorBase<Date>
 {

	private final static Logger LOG = LogManager.getLogger();
	public final static DateFormat FMT_DATE = new SimpleDateFormat("yyyy/MM/dd", LogUtil.LOCALE_DEFAULT);

	private final AsyncLongGenerator longGenerator;

	public AsyncDateGenerator(Date minValue, Date maxValue) {
		super(minValue, maxValue);
		longGenerator = new AsyncLongGenerator(minValue.getTime(), maxValue.getTime());
	}

	public AsyncDateGenerator(Date initialValue) throws ParseException{
		super(initialValue);
		longGenerator = new AsyncLongGenerator(
			FMT_DATE.parse("1970/01/01").getTime(), initialValue.getTime()
		);
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
		return new Date(longGenerator.get());
	}

	@Override
	public boolean isInitialized() {
		return longGenerator != null;
	}
}
