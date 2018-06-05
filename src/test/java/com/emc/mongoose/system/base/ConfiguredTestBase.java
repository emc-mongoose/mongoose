package com.emc.mongoose.system.base;

import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 19.01.17.
 */
public abstract class ConfiguredTestBase
extends LoggingTestBase {

	protected final List<String> configArgs = new ArrayList<>();

	protected ConfiguredTestBase(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, runMode, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		configArgs.add("--item-data-size=" + itemSize.getValue());
		configArgs.add("--load-step-id=" + stepId);
		configArgs.add("--load-step-idTmp=false");
		configArgs.add("--load-step-limit-concurrency=" + concurrency.getValue());
		configArgs.add("--output-metrics-trace-persist=true");
		configArgs.add("--storage-driver-type=" + storageType.name().toLowerCase());
	}

	@After
	public void tearDown()
	throws Exception {
		configArgs.clear();
		super.tearDown();
	}
}
