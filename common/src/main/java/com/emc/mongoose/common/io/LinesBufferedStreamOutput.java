package com.emc.mongoose.common.io;

import com.emc.mongoose.common.Constants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 Created by kurila on 15.11.16.
 */
public abstract class LinesBufferedStreamOutput
implements Output<String> {
	
	private static final ThreadLocal<StringBuilder> THRLOC_STRB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	
	private static final String LINE_SEP = System.getProperty("line.separator");
	
	protected final BufferedWriter writer;
	
	public LinesBufferedStreamOutput(final OutputStream out)
	throws IOException {
		writer = new BufferedWriter(new OutputStreamWriter(out), Constants.MIB);
	}
	
	@Override
	public boolean put(final String line)
	throws IOException {
		writer.write(line + LINE_SEP);
		return true;
	}
	
	@Override
	public int put(final List<String> lines, final int from, final int to)
	throws IOException {
		final StringBuilder strb = THRLOC_STRB.get();
		strb.setLength(0);
		for(int i = from; i < to; i ++) {
			strb.append(lines.get(i));
			strb.append(LINE_SEP);
		}
		writer.write(strb.toString());
		return to - from;
	}
	
	@Override
	public int put(final List<String> lines)
	throws IOException {
		final StringBuilder strb = THRLOC_STRB.get();
		strb.setLength(0);
		for(final String line : lines) {
			strb.append(line);
			strb.append(LINE_SEP);
		}
		writer.write(strb.toString());
		return lines.size();
	}
	
	@Override
	public void close()
	throws IOException {
		writer.close();
	}
}
