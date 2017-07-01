package com.emc.mongoose.common;

import java.util.Locale;

/**
 Created on 11.07.16.
 */
public interface Constants {

	String DIR_CONFIG = "config";

	String FNAME_CONFIG = "defaults.json";

	String KEY_BASE_DIR = "base.dir";

	String KEY_TEST_ID = "test.id";

	String KEY_TEST_STEP_ID = "test.step.id";
	
	String KEY_CLASS_NAME = "class.name";

	int MIB = 0x10_00_00;

	double K = 1e3;
	double M = 1e6;

	Locale LOCALE_DEFAULT = Locale.ROOT;
}
