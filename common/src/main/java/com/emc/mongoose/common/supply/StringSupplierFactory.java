package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.io.Input;

import java.io.File;

public class StringSupplierFactory<G extends BatchSupplier<String>>
implements SupplierFactory<String, G> {

	public static final String PATH_REG_EXP = "([0-9a-z]+" + "\\" + File.separatorChar + ")+";

	private static final StringSupplierFactory<? extends Input<String>>
			INSTANCE = new StringSupplierFactory<>();

	private StringSupplierFactory() {
	}

	public static StringSupplierFactory<? extends Input<String>> getInstance() {
		return INSTANCE;
	}
	
	@Override
	public State defineState(final String... parameters) {
		if(parameters[0] == null) {
			if(parameters[1] == null) {
				return State.EMPTY;
			} else {
				return State.RANGE;
			}
		} else {
			if(parameters[1] == null) {
				return State.FORMAT;
			} else {
				return State.FORMAT_RANGE;
			}
		}
	}

	@Override @SuppressWarnings("unchecked")
	public G createSupplier(final char type, final String... parameters)
	throws IllegalArgumentException {
		final State state =  (State) defineState(parameters);
		switch (state) {
			case FORMAT:
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
