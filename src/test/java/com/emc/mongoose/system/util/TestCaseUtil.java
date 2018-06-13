package com.emc.mongoose.system.util;

import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;

import com.google.common.base.CaseFormat;

public interface TestCaseUtil {

	static String snakeCaseName(final Class testCaseCls) {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, testCaseCls.getSimpleName());
	}

	static String stepId(
		final Class testCaseCls, final StorageType storageType, final RunMode runMode,
		final Concurrency concurrency, final ItemSize itemSize
	) {
		return snakeCaseName(testCaseCls) + '_' + storageType.name() + '_'
			+ runMode.name() + 'x' + concurrency.name() + '_' + itemSize.name();
	}
}
