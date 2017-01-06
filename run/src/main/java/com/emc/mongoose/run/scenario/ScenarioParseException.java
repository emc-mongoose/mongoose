package com.emc.mongoose.run.scenario;

/**
 Created by andrey on 06.01.17.
 */
public final class ScenarioParseException
extends Exception {

	public ScenarioParseException() {
		super();
	}

	public ScenarioParseException(final String msg) {
		super(msg);
	}

	public ScenarioParseException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
