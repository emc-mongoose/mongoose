package com.emc.mongoose.core.impl.load.model.reader;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by olga on 08.05.15.
 */
public abstract class FileReader
extends BufferedReader{

	public FileReader(Reader in) {
		super(in);
	}

	public abstract String getLine()
	throws IOException;

	public final void close() throws IOException {
		super.close();
	}
}
