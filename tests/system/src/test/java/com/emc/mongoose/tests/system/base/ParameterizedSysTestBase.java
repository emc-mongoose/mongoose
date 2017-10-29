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
import java.util.Map;

import static org.junit.runners.Parameterized.Parameters;

/**
 Created by andrey on 11.08.17.
 */

@RunWith(Parameterized.class)
public abstract class ParameterizedSysTestBase {

	@Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> data() {

		final Map<String, String> env = System.getenv();

		final List<StorageType> includedStorageTypes = new ArrayList<>();
		final String storageTypeValues = env.get(StorageType.KEY_ENV);
		for(final String storageTypeValue : storageTypeValues.split(",")) {
			includedStorageTypes.add(StorageType.valueOf(storageTypeValue.toUpperCase()));
		}

		final List<DriverCount> includedDriverCounts = new ArrayList<>();
		final String driverCountValues = env.get(DriverCount.KEY_ENV);
		for(final String driverCountValue : driverCountValues.split(",")) {
			includedDriverCounts.add(DriverCount.valueOf(driverCountValue.toUpperCase()));
		}

		final List<Concurrency> includedConcurrencies = new ArrayList<>();
		final String concurrencyValues = env.get(Concurrency.KEY_ENV);
		for(final String concurrencyValue : concurrencyValues.split(",")) {
			includedConcurrencies.add(Concurrency.valueOf(concurrencyValue.toUpperCase()));
		}

		final List<ItemSize> includedItemSizes = new ArrayList<>();
		final String itemSizeValues = env.get(ItemSize.KEY_ENV);
		for(final String itemSizeValue : itemSizeValues.split(",")) {
			includedItemSizes.add(ItemSize.valueOf(itemSizeValue.toUpperCase()));
		}

		final List<Object[]> data = new ArrayList<>();
		for(final StorageType storageType : includedStorageTypes) {
			for(final DriverCount driverCount : includedDriverCounts) {
				for(final Concurrency concurrency : includedConcurrencies) {
					for(final ItemSize itemSize : includedItemSizes) {
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
	}

	@Test
	public abstract void test()
	throws Exception;
}
