package com.emc.mongoose.common.generator;

public class StringGeneratorFactory implements GeneratorFactory<String> {

	private enum State {
		DEFAULT
	}

	@Override
	public Enum defineState(String... parameters) {
		return State.DEFAULT;
	}

	@Override
	public ValueGenerator<String> createGenerator(char type, String... parameters) throws Exception {
		State state =  (State) defineState(parameters);
		switch (state) {
			case DEFAULT:
				switch (type) {
					case 'p':
						return null; //todo replace with a real path generator
					default:
						throw new IllegalArgumentException();
				}
			default:
				throw new IllegalArgumentException();
		}
	}
}
