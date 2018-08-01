package com.emc.mongoose.system.base;

import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

		final List<RunMode> includedRunModes = new ArrayList<>();
		final String nodeCountValues = env.get(RunMode.KEY_ENV);
		for(final String nodeCountValue : nodeCountValues.split(",")) {
			includedRunModes.add(RunMode.valueOf(nodeCountValue.toUpperCase()));
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
			for(final RunMode runMode : includedRunModes) {
				for(final Concurrency concurrency : includedConcurrencies) {
					for(final ItemSize itemSize : includedItemSizes) {
						data.add(
							new Object[] {
								storageType, runMode, concurrency, itemSize
							}
						);
					}
				}
			}
		}

		return data;
	}

	protected final StorageType storageType;
	protected final RunMode runMode;
	protected final Concurrency concurrency;
	protected final ItemSize itemSize;

	protected ParameterizedSysTestBase(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final ItemSize itemSize
	) {
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		System.out.println(
			getClass().getSimpleName() + " params: "
				+ StorageType.KEY_ENV + " = " + storageType.name() + ", "
				+ RunMode.KEY_ENV + " = " + runMode.name() + ", "
				+ Concurrency.KEY_ENV + " = " + concurrency.name() + ", "
				+ ItemSize.KEY_ENV + " = " + itemSize.name()
		);
	}

	@Test
	public abstract void test()
	throws Exception;
}
