package com.emc.mongoose.system;

import com.emc.mongoose.system.base.ScenarioTestBase;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.NodeCount;
import com.emc.mongoose.system.base.params.StorageType;

public class CircularAppendTest
extends ScenarioTestBase {

	protected CircularAppendTest(
		final StorageType storageType, final NodeCount nodeCount, final Concurrency concurrency,
		final ItemSize itemSize, final int storageNodePort, final String itemInputFile,
		final String itemNamingPrefix, final int itemNamingRadix, final boolean sslFlag
	) throws Exception {
		super(
			storageType, nodeCount, concurrency, itemSize, storageNodePort, itemInputFile,
			itemNamingPrefix, itemNamingRadix, sslFlag
		);
	}

	@Override
	protected String makeScenarioPath() {
		return "js/systest/circular_append.js";
	}

	@Override
	protected String makeStepId() {
		return getClass().getSimpleName() + '-' + storageType.name() + '-' +
			nodeCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	public void test()
	throws Exception {

	}
}
