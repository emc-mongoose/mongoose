package com.emc.mongoose.concurrent;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 Modifies the async runnable to make sure that all instances are closed even if an user hits ^C
 */
public abstract class AutoCloseOnShutdownBase
extends AsyncRunnableBase {

	private static final Logger LOG = Logger.getLogger(AutoCloseOnShutdownBase.class.getSimpleName());

	protected AutoCloseOnShutdownBase() {
	}

	@Override
	public final void close()
	throws IOException {
		super.close();
	}
}
