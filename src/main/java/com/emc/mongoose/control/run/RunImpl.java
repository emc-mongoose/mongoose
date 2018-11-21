package com.emc.mongoose.control.run;

import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public final class RunImpl
implements Run {

	private long startTimeMillis = -1;

	private final String comment;
	private final String scenario;
	private final ScriptEngine scriptEngine;

	public RunImpl(final String comment, final String scenario, final ScriptEngine scriptEngine) {
		this.comment = comment;
		this.scenario = scenario;
		this.scriptEngine = scriptEngine;
	}

	@Override
	public final long startTimeMillis() {
		if(startTimeMillis > 0) {
			return startTimeMillis;
		} else {
			throw new IllegalStateException("Not started yet");
		}
	}

	@Override
	public final String comment() {
		return comment;
	}

	@Override
	public final void run() {
		startTimeMillis = System.currentTimeMillis();
		Loggers.SCENARIO.log(Level.INFO, scenario);
		try {
			scriptEngine.eval(scenario);
		} catch(final ScriptException e) {
			LogUtil.trace(
				Loggers.ERR, Level.ERROR, e, "\nScenario failed, line #{}, column #{}:\n{}", e.getLineNumber(),
				e.getColumnNumber(), e.getMessage()
			);
		}
	}
}
