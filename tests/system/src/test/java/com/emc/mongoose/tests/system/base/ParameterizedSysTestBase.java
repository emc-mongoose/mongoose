package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.StorageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static org.junit.runners.Parameterized.Parameters;

/**
 Created by andrey on 11.08.17.
 */

@RunWith(Parameterized.class)
public abstract class ParameterizedSysTestBase {

	@Parameters
	public static List<Object[]> data() {
		final List<Object[]> data = new ArrayList<>();
		for(final StorageType storageType : StorageType.values()) {
			for(final DriverCount driverCount : DriverCount.values()) {
				for(final Concurrency concurrency : Concurrency.values()) {
					for(final ItemSize itemSize : ItemSize.values()) {
						data.add(
							new Object[] {
								storageType, driverCount, concurrency, itemSize
							}
						);
					}
				}
			}
		}
		return data;
	}

	protected final StorageType storageType;
	protected final DriverCount driverCount;
	protected final Concurrency concurrency;
	protected final ItemSize itemSize;

	protected ParameterizedSysTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) {
		this.storageType = storageType;
		this.driverCount = driverCount;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		assumeApplicable();
	}

	protected abstract void assumeApplicable();

	@Test
	public abstract void test()
	throws Exception;
}
