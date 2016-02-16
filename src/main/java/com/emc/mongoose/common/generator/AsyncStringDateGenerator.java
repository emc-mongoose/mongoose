package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class AsyncStringDateGenerator extends AsyncRangeGeneratorBase<String> {

	private final static Logger LOG = LogManager.getLogger();
	public final static DateFormat FMT_DATE = new SimpleDateFormat("yyyy/MM/dd", LogUtil.LOCALE_DEFAULT); //todo temp

	private AsyncDateGenerator dateGenerator = null;

	public AsyncStringDateGenerator(String minValue, String maxValue) {
		super(minValue, maxValue);
		try {
			dateGenerator = new AsyncDateGenerator(
							FMT_DATE.parse(minValue),
							FMT_DATE.parse(maxValue));
		} catch (ParseException e) {
			e.printStackTrace();
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
			e.printStackTrace();
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
