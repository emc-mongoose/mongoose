package com.emc.mongoose.tests.system.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created by olga on 02.07.15.
 */
public final class BufferingOutputStream
extends ByteArrayOutputStream {

	private final PrintStream replacedStream;

	public BufferingOutputStream(final PrintStream replacedStream) {
		this.replacedStream = replacedStream;
	}

	public final void startRecording() {
		synchronized(System.out) {
			System.out.flush();
			System.setOut(new PrintStream(this));
		}
	}

	public final String stopRecording() {
		synchronized(System.out) {
			System.out.flush();
			System.setOut(replacedStream);
		}
		return toString();
	}

	@Override
	public final synchronized void write(final int b) {
		super.write(b);
		replacedStream.write(b);
	}

	@Override
	public final synchronized void write(final byte[] b, final int off, final int len) {
		super.write(b, off, len);
		replacedStream.write(b, off, len);
	}
}
