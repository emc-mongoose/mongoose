package com.emc.mongoose.logging;

import com.emc.mongoose.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

import java.text.NumberFormat;

import static java.lang.ThreadLocal.withInitial;

/**
 Created by kurila on 26.10.16.
 */
public abstract class LogMessageBase
implements Message, StringBuilderFormattable {
	
	private static final ThreadLocal<StringBuilder> THRLOC_STRB = withInitial(StringBuilder::new);
	
	@Override
	public final String getFormattedMessage() {
		final StringBuilder strb = THRLOC_STRB.get();
		strb.setLength(0);
		formatTo(strb);
		return strb.toString();
	}
	
	@Override
	public final String getFormat() {
		return "";
	}
	
	@Override
	public final Object[] getParameters() {
		return null;
	}
	
	@Override
	public final Throwable getThrowable() {
		return null;
	}
	
	protected static String formatFixedWidth(final double value, final int count) {
		final String result;
		if(value > Math.pow(10, count - 2) && value < Math.pow(10, count)) {
			result = Long.toString((long) value);
		} else if(value >= Math.pow(10, count)) {
			final String t = Double.toString(value);
			result = t.substring(0, count - 2) + t.substring(t.length() - 2);
		} else {
			final String valueStr = Double.toString(value);
			if(value < Math.pow(10, count) && valueStr.length() > count) {
				result = valueStr.substring(0, count);
			} else if(value < Math.pow(10, - count + 2)) {
				result = "0";
			} else {
				result = valueStr;
			}
		}
		return result;
	}
}
