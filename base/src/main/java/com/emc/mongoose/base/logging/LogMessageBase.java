package com.emc.mongoose.base.logging;

import static java.lang.ThreadLocal.withInitial;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/** Created by kurila on 26.10.16. */
public abstract class LogMessageBase implements Message, StringBuilderFormattable {

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
		final String t = Double.toString(value);
		if (value > Math.pow(10, count - 2) && value < Math.pow(10, count)) {
			result = Long.toString((long) value);
		} else if (value >= Math.pow(10, count)) {
			result = t.substring(0, count - 2) + t.substring(t.length() - 2);
		} else {
			if (t.length() > count) {
				if (t.startsWith("0.") || value >= 1) {
					result = t.substring(0, count);
				} else if (value < 1.e-99) {
					result = "0";
				} else {
					result = t.substring(0, count - 3) + t.substring(t.length() - 3);
				}
			} else {
				result = t;
			}
		}
		return result;
	}
}
