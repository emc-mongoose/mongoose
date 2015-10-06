package com.emc.mongoose.integ.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by olga on 02.07.15.
 */
public final class BufferingOutputStream
extends ByteArrayOutputStream {

	private final PrintStream replacedStream;

	public BufferingOutputStream(final PrintStream out) {
		super();
		this.replacedStream = out;
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

	@Override
	public void close()
	throws IOException {
		System.setOut(replacedStream);
		super.close();
	}
}
