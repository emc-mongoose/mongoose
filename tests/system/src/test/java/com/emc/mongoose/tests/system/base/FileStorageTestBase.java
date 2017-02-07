package com.emc.mongoose.tests.system.base;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 Created by andrey on 07.02.17.
 */
public abstract class FileStorageTestBase
extends ConfiguredTestBase {

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		CONFIG_ARGS.add("--storage-type=fs");
		ConfiguredTestBase.setUpClass();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		ConfiguredTestBase.tearDownClass();
	}
}
