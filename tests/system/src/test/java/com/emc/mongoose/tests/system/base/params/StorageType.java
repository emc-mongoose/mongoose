package com.emc.mongoose.tests.system.base.params;

/**
 Created by andrey on 11.08.17.
 */

public enum StorageType {
	AMZS3,
	ATMOS,
	FS,
	SWIFT;

	public static final String KEY_ENV = "STORAGE_TYPE";
}