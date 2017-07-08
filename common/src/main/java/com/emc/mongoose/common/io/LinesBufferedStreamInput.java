package com.emc.mongoose.common.io;

import com.emc.mongoose.common.Constants;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 Created by kurila on 15.11.16.
 */
public class LinesBufferedStreamInput
implements Input<String> {
	
	private final BufferedReader reader;
	
	public LinesBufferedStreamInput(final InputStream input) {
		reader = new BufferedReader(new InputStreamReader(input), Constants.MIB);
	}
	
	@Override
	public String get()
	throws EOFException, IOException {
		return reader.readLine();
	}
	
	@Override
	public int get(final List<String> buffer, final int limit)
	throws IOException {
		int i = 0;
		try {
			for(; i < limit; i ++) {
				buffer.add(reader.readLine());
			}
		} catch(final IOException e) {
			if(i == 0) {
				throw e;
			}
		}
		return i;
	}
	
	/**
	 Skips bytes instead of items
	 */
	@Override
	public long skip(final long count)
	throws IOException {
		return reader.skip(count);
	}
	
	/**
	 Most probably will cause an IOException
	 @throws IOException
	 */
	@Override
	public void reset()
	throws IOException {
		reader.reset();
	}
	
	@Override
	public void close()
	throws IOException {
		reader.close();
	}
}
