package com.emc.mongoose.ui.config.storage.driver.queue;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 25.07.17.
 */
public final class QueueConfig
implements Serializable {

	public static final String KEY_INPUT = "input";
	public static final String KEY_OUTPUT = "output";

	public final void setInput(final int input) {
		this.input = input;
	}

	public final void setOutput(final int output) {
		this.output = output;
	}

	@JsonProperty(KEY_INPUT) private int input;
	@JsonProperty(KEY_OUTPUT) private int output;

	public QueueConfig() {
	}

	public QueueConfig(final QueueConfig other) {
		this.input = other.getInput();
		this.output = other.getOutput();
	}

	public final int getInput() {
		return input;
	}

	public final int getOutput() {
		return output;
	}
}
