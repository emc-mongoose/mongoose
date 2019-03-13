package com.emc.mongoose.base;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

public interface Exceptions {

	static void throwUncheckedIfInterrupted(final Throwable t) {
		if (t instanceof InterruptedException) {
			throwUnchecked(t);
		}
	}
}
