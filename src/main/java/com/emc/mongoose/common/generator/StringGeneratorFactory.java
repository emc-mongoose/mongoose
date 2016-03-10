package com.emc.mongoose.common.generator;

public class StringGeneratorFactory implements GeneratorFactory<String> {

	private static StringGeneratorFactory singleton = null;

	private StringGeneratorFactory() {
	}

	public static StringGeneratorFactory generatorFactory() {
		if (singleton == null) {
			singleton = new StringGeneratorFactory();
		}
		return singleton;
	}

	private enum State {
		DEFAULT
	}

	@Override
	public Enum defineState(final String... parameters) {
		return State.DEFAULT;
	}

	@Override
	public ValueGenerator<String> createGenerator(final char type, final String... parameters) {
		State state =  (State) defineState(parameters);
		switch (state) {
			case DEFAULT:
				switch (type) {
					case 'p':
						return new FilePathGenerator(parameters[0]);
					default:
						throw new IllegalArgumentException();
				}
			default:
				throw new IllegalArgumentException();
		}
	}
}
