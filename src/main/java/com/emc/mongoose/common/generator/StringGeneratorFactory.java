package com.emc.mongoose.common.generator;

public class StringGeneratorFactory<G extends ValueGenerator<String>>
implements GeneratorFactory<String, G> {

	private static final StringGeneratorFactory<? extends ValueGenerator<String>>
			INSTANCE = new StringGeneratorFactory<>();

	private StringGeneratorFactory() {
	}

	public static StringGeneratorFactory<? extends ValueGenerator<String>> getInstance() {
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
	public G createGenerator(final char type, final String... parameters) {
		final State state =  (State) defineState(parameters);
		switch (state) {
			case DEFAULT:
				switch (type) {
					case 'p':
						return (G) new FilePathGenerator(parameters[0]);
					default:
						throw new IllegalArgumentException();
				}
			default:
				throw new IllegalArgumentException();
		}
	}
}
