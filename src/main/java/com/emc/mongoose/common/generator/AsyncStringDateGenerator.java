package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.FMT_DATE;

public class AsyncStringDateGenerator extends AsyncRangeGeneratorBase<String> {

	private final static Logger LOG = LogManager.getLogger();

	private AsyncDateGenerator dateGenerator;

	public AsyncStringDateGenerator(String minValue, String maxValue) {
		super(minValue, maxValue);
		try {
			dateGenerator = new AsyncDateGenerator(
							FMT_DATE.parse(minValue),
							FMT_DATE.parse(maxValue));
		} catch (ParseException e) {
			LogUtil.exception(
					LOG, Level.WARN, e, "Failed to parse the date."
			);
		}
	}

	public AsyncStringDateGenerator(String initialValue) {
		super(initialValue);
		try {
			dateGenerator = new AsyncDateGenerator(
					FMT_DATE.parse(initialValue));
		} catch (ParseException e) {
			LogUtil.exception(
					LOG, Level.WARN, e, "Failed to parse the date."
			);
		}
	}

	@Override
	protected String computeRange(String minValue, String maxValue) {
		return null;
	}

	@Override
	protected String rangeValue() {
		return FMT_DATE.format(dateGenerator.get());
	}

	@Override
	protected String singleValue() {
		return FMT_DATE.format(dateGenerator.get());
	}
}
