package com.emc.mongoose.common.io.value;

import com.emc.mongoose.common.io.Input;

import java.io.File;

public class StringInputFactory<G extends Input<String>>
implements ValueInputFactory<String, G> {

	public static final String PATH_REG_EXP = "([0-9a-z]+" + "\\" + File.separatorChar + ")+";

	private static final StringInputFactory<? extends Input<String>>
			INSTANCE = new StringInputFactory<>();

	private StringInputFactory() {
	}

	public static StringInputFactory<? extends Input<String>> getInstance() {
		return INSTANCE;
	}

	private enum State {
		DEFAULT
	}

	@Override
	public Enum defineState(final String... parameters) {
		return State.DEFAULT;
	}

	@Override @SuppressWarnings("unchecked")
	public G createInput(final char type, final String... parameters)
	throws IllegalArgumentException {
		final State state =  (State) defineState(parameters);
		switch (state) {
			case DEFAULT:
				switch (type) {
					case 'p':
						return (G) new FilePathInput(parameters[0]);
					default:
						throw new IllegalArgumentException();
				}
			default:
				throw new IllegalArgumentException();
		}
	}
}
