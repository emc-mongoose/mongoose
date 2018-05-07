package com.emc.mongoose;

import java.io.File;
import java.util.Locale;

/**
 Created on 11.07.16.
 */
public interface Constants {

	String APP_NAME = "mongoose";
	String USER_HOME = System.getProperty("user.home");
	String DIR_CONFIG = "config";
	String DIR_EXAMPLE = "example";
	String DIR_EXAMPLE_SCENARIO = DIR_EXAMPLE + File.separator + "scenario";
	String PATH_DEFAULTS = DIR_CONFIG + File.separator + "defaults.json";
	String KEY_HOME_DIR = "homeDir";
	String KEY_TEST_STEP_ID = "stepId";
	String KEY_CLASS_NAME = "className";
	int MIB = 0x10_00_00;
	double K = 1e3;
	double M = 1e6;
	Locale LOCALE_DEFAULT = Locale.ROOT;
}
