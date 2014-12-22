package com.emc.mongoose.util.pool;
//
import java.io.Closeable;
/**
 Created by kurila on 22.12.14.
 */
public interface Reusable
extends Closeable {
	Reusable reuse(final Object... args);
}
