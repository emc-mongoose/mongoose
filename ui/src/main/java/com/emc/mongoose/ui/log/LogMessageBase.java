package com.emc.mongoose.ui.log;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 Created by kurila on 26.10.16.
 */
public abstract class LogMessageBase
implements Message, StringBuilderFormattable {
	
	private static final ThreadLocal<StringBuilder> THRLOC_STRB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	
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
}
