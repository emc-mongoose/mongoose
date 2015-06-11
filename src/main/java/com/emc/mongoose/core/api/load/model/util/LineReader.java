package com.emc.mongoose.core.api.load.model.util;

import java.io.Closeable;

/**
 * Created by olga on 20.05.15.
 */
public interface LineReader
extends Readable, Closeable{
	String readLine();
}
