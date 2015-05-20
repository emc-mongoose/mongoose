package com.emc.mongoose.core.impl.load.model.reader.io;

import java.io.Closeable;

/**
 * Created by olga on 20.05.15.
 */
public interface LineReader
extends Readable, Closeable{

	String readLine();

	void close();
}
