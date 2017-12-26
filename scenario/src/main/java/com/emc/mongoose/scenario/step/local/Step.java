package com.emc.mongoose.scenario.step.local;

import com.emc.mongoose.scenario.step.Pausible;
import com.emc.mongoose.scenario.step.Resumable;
import com.emc.mongoose.scenario.step.StepSlice;

import java.io.Closeable;

public interface Step
extends Runnable, Closeable, Pausible, Resumable, Sliceable<StepSlice> {
}
