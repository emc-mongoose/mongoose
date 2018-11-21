package com.emc.mongoose.concurrent;

import java.io.Closeable;
import java.util.concurrent.Executor;

public interface TaskExecutor
extends Executor, Closeable {
}
