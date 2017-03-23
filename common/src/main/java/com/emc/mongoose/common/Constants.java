package com.emc.mongoose.common;

import java.util.Locale;

/**
 Created on 11.07.16.
 */
public interface Constants {

	String DIR_CONFIG = "config";

	String FNAME_CONFIG = "defaults.json";

	String FNAME_LOG_CONFIG = "logging.json";

	String KEY_STEP_NAME = "step.name";

	int MIB = 0x10_00_00;

	double K = 1e3;
	double M = 1e6;

	Locale LOCALE_DEFAULT = Locale.ROOT;

	int BATCH_SIZE = 0x1000;
}
