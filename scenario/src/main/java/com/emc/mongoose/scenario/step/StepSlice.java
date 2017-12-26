package com.emc.mongoose.scenario.step;

import java.io.Closeable;
import java.io.Serializable;

public interface StepSlice
extends Pausible, Resumable, Runnable, Serializable, Closeable {
}
