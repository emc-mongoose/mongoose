package com.emc.mongoose.common.io;

import com.emc.mongoose.common.io.Input;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 24.10.16.
 */
public class ConstantStringInput
implements Input<String> {

	protected final String value;

	public ConstantStringInput(final String value) {
		this.value = value;
	}

	@Override
	public String get()
	throws EOFException, IOException {
		return value;
	}

	@Override
	public int get(final List<String> buffer, final int limit)
	throws IOException {
		for(int i = 0; i < limit; i ++) {
			buffer.add(value);
		}
		return limit;
	}

	@Override
	public long skip(final long count)
	throws IOException {
		return 0;
	}

	@Override
	public void reset()
	throws IOException {
	}

	@Override
	public void close()
	throws IOException {
	}
}
