package com.emc.mongoose.scenario.step.local;

import com.emc.mongoose.scenario.step.Startable;
import com.emc.mongoose.scenario.step.Stoppable;

import java.io.Closeable;

public interface Step
extends Closeable, Startable, Stoppable {
}
