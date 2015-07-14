package com.emc.mongoose.integ.tools;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created by olga on 02.07.15.
 */
public final class SavedOutputStream
extends ByteArrayOutputStream {

	private final PrintStream out;

	public SavedOutputStream(final PrintStream out){
		super();
		this.out = out;
	}

	@Override
	public synchronized void write(final int b) {
		super.write(b);
		out.write(b);
	}

	@Override
	public synchronized void write(final byte[] b, final int off, final int len) {
		super.write(b, off, len);
		out.write(b, off, len);
	}

	public final PrintStream getPrintStream(){
		return  this.out;
	}
}
