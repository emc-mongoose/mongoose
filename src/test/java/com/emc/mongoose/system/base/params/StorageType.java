package com.emc.mongoose.system.base.params;

/**
 Created by andrey on 11.08.17.
 */

public enum StorageType {

	S3,
	ATMOS,
	EMCS3,
	FS,
	SWIFT;

	public static final String KEY_ENV = "STORAGE_TYPE";
}
