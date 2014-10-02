package com.emc.mongoose.util;
//
import java.io.OutputStream;
/**
 Created by kurila on 11.09.14.
 An output stream which doesn nothing.
 */
public final class NullOutputStream
extends OutputStream {
	@Override
	public final void write(final int i) {
	}
}
