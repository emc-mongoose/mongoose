package com.emc.mongoose.api.common;

import java.util.Locale;

/**
 Created on 11.07.16.
 */
public interface Constants {

	String DIR_CONFIG = "config";

	String FNAME_CONFIG = "defaults.json";

	String KEY_BASE_DIR = "baseDir";

	String KEY_TEST_STEP_ID = "stepId";
	
	String KEY_CLASS_NAME = "className";

	int MIB = 0x10_00_00;

	double K = 1e3;
	double M = 1e6;

	Locale LOCALE_DEFAULT = Locale.ROOT;
}
