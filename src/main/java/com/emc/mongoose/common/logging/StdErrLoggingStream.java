package com.emc.mongoose.common.logging;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

import java.io.ByteArrayOutputStream;

/**
 * Created by gusakk on 4/29/15.
 */
public final class StdErrLoggingStream
extends ByteArrayOutputStream {
	//
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final int DEFAULT_BUFFER_LENGTH = 2048;
	//
	private int curBufLength;
	private final Logger log;
	private final Marker marker;
	//
	public StdErrLoggingStream(final Logger log,
	                           final Marker marker)
			throws IllegalArgumentException {
		super(DEFAULT_BUFFER_LENGTH);
		if (log == null || marker == null) {
			throw new IllegalArgumentException(
					"Logger or log marker must be not null");
		}
		this.log = log;
		this.marker = marker;
		curBufLength = DEFAULT_BUFFER_LENGTH;
		count = 0;
	}
	//
	public final synchronized void write(final int b) {
		if (b == 0) {
			return;
		}
		if (count - curBufLength > 0) {
			ensureCapacity();
		}
		buf[count++] = (byte) b;
	}
	//
	public final synchronized void write(byte[] b, int off, int len) {
		if(off >= 0 && off <= b.length && len >= 0 && off + len - b.length <= 0) {
			if ((count + len) - curBufLength > 0) {
				ensureCapacity();
			}
			System.arraycopy(b, off, this.buf, this.count, len);
			this.count += len;
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	//
	public final synchronized void ensureCapacity() {
		final int newBufLength = curBufLength +
				DEFAULT_BUFFER_LENGTH;
		final byte[] newBuf = new byte[newBufLength];
		System.arraycopy(buf, 0, newBuf, 0, curBufLength);
		buf = newBuf;
		curBufLength = newBufLength;
	}
	//
	public final void flush() {
		if (count == 0) {
			return;
		}
		if (count == LINE_SEPARATOR.length()) {
			if (((char) buf[0]) == LINE_SEPARATOR.charAt(0)  &&
					(( count == 1 ) ||  // <- Unix & Mac, -> Windows
						(( count == 2 ) && ((char)buf[1]) == LINE_SEPARATOR.charAt(1)))) {
				reset();
				return;
			}
		}
		//
		final byte[] bytes = new byte[count];
		System.arraycopy(buf, 0, bytes, 0, count);
		final String logEvent = new String(bytes);
		log.debug(marker, logEvent);
		count = 0;
	}
	//
	public final void close() {
		flush();
	}
}
