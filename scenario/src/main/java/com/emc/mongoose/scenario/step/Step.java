package com.emc.mongoose.scenario.step;

import com.emc.mongoose.scenario.ScenarioParseException;

import java.io.Closeable;
import java.util.Map;

/**
 A runnable step configuration container. The collected configuration is applied upon invocation.
 */
public interface Step
extends Closeable, Runnable {
}
